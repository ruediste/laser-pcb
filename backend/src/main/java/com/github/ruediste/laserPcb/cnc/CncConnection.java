package com.github.ruediste.laserPcb.cnc;

import static java.util.stream.Collectors.joining;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.ruediste.laserPcb.Observable;

public class CncConnection {
	private final Logger log = LoggerFactory.getLogger(CncConnection.class);

	public SerialConnection con;
	private CncState state = new CncState();
	private volatile boolean closing;
	private CountDownLatch closed = new CountDownLatch(1);
	private Object lock = new Object();

	public final Observable<Void> gCodeCompleted = new Observable<>();

	volatile private boolean connectionOpened;
	private CncFirmwareType firmwareType;

	public static class InFlightGCode {
		public byte[] gCodeBytes;
		public String gCodeString;
		public Runnable onCompletion;

		public InFlightGCode(byte[] gCodeBytes, String gCodeString, Runnable onCompletion) {
			this.gCodeBytes = gCodeBytes;
			this.gCodeString = gCodeString;
			this.onCompletion = onCompletion;
		}

	}

	private Deque<InFlightGCode> inFlightGCodes = new ArrayDeque<>();
	int controllerReceiveBufferSize = 96;

	public CncConnection(SerialConnection con) {
		this.con = con;
		new Thread(this::readLoop, "serialReader " + con.port).start();
	}

	static Pattern statusLinePatternGrbl = Pattern.compile(
			"<(?<status>[A-Z][a-z]+)\\|MPos:(?<x>-?\\d+\\.\\d+),(?<y>-?\\d+\\.\\d+),(?<z>-?\\d+\\.\\d+)(\\|[^>]*)*>");
	static Pattern statusLinePatternMarlin = Pattern
			.compile("X:(?<x>-?\\d+\\.\\d+) Y:(?<y>-?\\d+\\.\\d+) Z:(?<z>-?\\d+\\.\\d+).*");

	private void readLoop() {
		log.info("Starting to read from serial port {}", con.port);
		try {

			StringBuilder sb = new StringBuilder();
			while (!closing) {
				int ch = con.in.read();
				if (ch == -1)
					break;

				if (ch == '\n') {
					String line = sb.toString();
					sb.setLength(0);
					log.info("Received {}", line);

					if (!connectionOpened) {
						if (line.startsWith("Grbl")) {
							log.info("Connected to GRBL firmware");
							firmwareType = CncFirmwareType.GRBL;
							connectionOpened = true;
						} else if (line.startsWith("Marlin")) {
							log.info("Connected to Marlin firmware");
							firmwareType = CncFirmwareType.MARLIN;
							connectionOpened = true;
						} else
							log.info("Receved line {}", line);
					} else {
						Matcher matcher;
						switch (firmwareType) {
						case GRBL:
							matcher = statusLinePatternGrbl.matcher(line);
							break;
						case MARLIN:
							matcher = statusLinePatternMarlin.matcher(line);
							break;
						default:
							throw new UnsupportedOperationException();
						}
						if (matcher.matches()) {
							log.info("Status received: {}", line);
							synchronized (state) {
								state.x = Double.parseDouble(matcher.group("x"));
								state.y = Double.parseDouble(matcher.group("y"));
								state.z = Double.parseDouble(matcher.group("z"));
							}
						} else if (line.toLowerCase(Locale.ENGLISH).startsWith("error")
								|| line.toLowerCase(Locale.ENGLISH).startsWith("ok")) {
							InFlightGCode gCode;
							int totalInFlightGCodeSize;
							synchronized (lock) {
								// gcode done, not in flight anymore
								gCode = inFlightGCodes.removeFirst();
								totalInFlightGCodeSize = totalInFlightGCodeSize();
								lock.notifyAll();
							}
							log.info("Response for {}: \"{}\" inFlight: {}",
									removeTrailingNewlines(new String(gCode.gCodeBytes, StandardCharsets.UTF_8)), line,
									totalInFlightGCodeSize);
							try {
								if (gCode.onCompletion != null)
									gCode.onCompletion.run();
							} catch (Throwable t) {
								log.error("Error in callback", t);
							}

							gCodeCompleted.send(null);

						} else if (removeTrailingNewlines(line).isEmpty()) {
							// swallow
						} else {
							log.info("received line {}", line);
						}
					}

				} else if (ch == '\r') {
					// swallow
				} else
					sb.append((char) ch);
			}
		} catch (Throwable t) {
			log.error("Error in read loop of port " + con.port, t);
		}
		closed.countDown();
		log.info("reading from serial port {} stopped", con.port);
	}

	/**
	 * Send a gCode
	 * 
	 * @return
	 */
	public boolean sendGCodeNonBlocking(String gCode, Runnable onCompletion) {
		gCode = removeTrailingNewlines(gCode);
		if (gCode.trim().isEmpty()) {
			if (onCompletion != null)
				onCompletion.run();
			return true;
		}

		byte[] bb = (gCode + "\n").getBytes(StandardCharsets.UTF_8);

		int totalSize;
		synchronized (lock) {
			totalSize = totalInFlightGCodeSize();
			totalSize += bb.length;
			if (totalSize < controllerReceiveBufferSize) {
				inFlightGCodes.addLast(new InFlightGCode(bb, gCode, onCompletion));
			} else
				return false;

		}

		log.info("Sending {}, inFlight: {}", gCode, totalSize);
		con.sendBytes(bb);
		return true;
	}

	/**
	 * Send a gCode
	 */
	public void sendGCode(String gCode) {
		sendGCode(gCode, null);
	}

	public void sendGCode(String gCode, Runnable onCompletion) {
		isJogging = false;
		sendGCodeImpl(gCode, onCompletion);
	}

	public void sendGCodeForJog(String gCode) {
		if (!isJogging) {
			isJogging = true;
			sendGCodeImpl("G91", null); // relative positioning
			sendGCodeImpl("G21", null); // set units to millimeters
			sendGCodeImpl("G0 F10000", null); // set feed
		}
		sendGCodeImpl(gCode, null);
	}

	private void sendGCodeImpl(String gCode, Runnable onCompletion) {
		gCode = removeTrailingNewlines(gCode);
		if (gCode.trim().isEmpty()) {
			if (onCompletion != null)
				onCompletion.run();
			return;
		}

		log.info("Sending {}", gCode);
		byte[] bb = (gCode + "\n").getBytes(StandardCharsets.UTF_8);

		synchronized (lock) {
			while (true) {
				int totalSize = totalInFlightGCodeSize();
				totalSize += bb.length;
				if (totalSize < controllerReceiveBufferSize) {
					inFlightGCodes.addLast(new InFlightGCode(bb, gCode, onCompletion));
					break;
				}
				try {
					lock.wait();
				} catch (InterruptedException e) {
					Thread.interrupted();
					throw new RuntimeException(e);
				}
			}
		}
		con.sendBytes(bb);
	}

	private boolean isJogging;

	private String removeTrailingNewlines(String s) {
		while (s.length() > 0 && (s.endsWith("\r") || s.endsWith("\n")))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	/**
	 * call only while holding the {@link #lock}
	 */
	private int totalInFlightGCodeSize() {
		return inFlightGCodes.stream().collect(Collectors.summingInt(x -> x.gCodeBytes.length));
	}

	public void close() {
		closing = true;
		con.close();
		try {
			closed.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		}
	}

	public static class CncState {
		public double x;
		public double y;
		public double z;

		public CncState() {
		}

		public CncState(CncState other) {
			this.x = other.x;
			this.y = other.y;
			this.z = other.z;
		}

	}

	private Semaphore statusPending = new Semaphore(1);

	public void queryStatus() {
		if (!connectionOpened)
			return;

		try {
			statusPending.acquire();
			// after two seconds just ignore the locked semaphore
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		String gCode = "$";
		switch (firmwareType) {
		case GRBL:
			gCode = "?";
			break;
		case MARLIN:
			gCode = "M114";
			break;
		default:
			throw new UnsupportedOperationException();
		}
		synchronized (lock) {
			log.info("In flight GCodes: {}", inFlightGCodes.stream().map(x -> x.gCodeString).collect(joining(",")));
		}
		sendGCode(gCode, () -> {
			if (statusPending.availablePermits() == 0)
				statusPending.release();
		});
	}

	public CncState getState() {
		synchronized (state) {
			return new CncState(state);
		}
	}

	public List<InFlightGCode> getInFlightGCodes() {
		synchronized (lock) {
			return new ArrayList<>(inFlightGCodes);
		}
	}

}
