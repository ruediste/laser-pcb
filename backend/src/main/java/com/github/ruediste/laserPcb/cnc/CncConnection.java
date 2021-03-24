package com.github.ruediste.laserPcb.cnc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
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

	private static class InFlightGCode {
		public byte[] gCode;
		public Runnable onCompletion;

		public InFlightGCode(byte[] gCode, Runnable onCompletion) {
			this.gCode = gCode;
			this.onCompletion = onCompletion;
		}

	}

	private Deque<InFlightGCode> inFlightGCodes = new ArrayDeque<>();
	int controllerReceiveBufferSize = 128;

	public CncConnection(SerialConnection con) {
		this.con = con;
		new Thread(this::readLoop, "serialReader " + con.port).start();
	}

	static Pattern statusLinePatternGrbl = Pattern.compile(
			"<(?<status>[A-Z][a-z]+)\\|MPos:(?<x>\\d+\\.\\d+),(?<y>\\d+\\.\\d+),(?<z>\\d+\\.\\d+)(\\|[^>]*)*>");
	static Pattern statusLinePatternMarlin = Pattern
			.compile("X:(?<x>\\d+\\.\\d+) Y:(?<y>\\d+\\.\\d+) Z:(?<z>\\d+\\.\\d+).*");

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
									removeTrailingNewlines(new String(gCode.gCode, StandardCharsets.UTF_8)), line,
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
		log.info("Sending {}", gCode);
		gCode += "\n";
		byte[] bb = gCode.getBytes(StandardCharsets.UTF_8);

		synchronized (lock) {
			int totalSize = totalInFlightGCodeSize();
			totalSize += bb.length;
			if (totalSize < controllerReceiveBufferSize) {
				inFlightGCodes.addLast(new InFlightGCode(bb, onCompletion));
			} else
				return false;

		}

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
		gCode = removeTrailingNewlines(gCode);
		log.info("Sending {}", gCode);
		gCode += "\n";
		byte[] bb = gCode.getBytes(StandardCharsets.UTF_8);

		synchronized (lock) {
			while (true) {
				int totalSize = totalInFlightGCodeSize();
				totalSize += bb.length;
				if (totalSize < controllerReceiveBufferSize) {
					inFlightGCodes.addLast(new InFlightGCode(bb, onCompletion));
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

	private String removeTrailingNewlines(String s) {
		while (s.length() > 0 && (s.endsWith("\r") || s.endsWith("\n")))
			s = s.substring(0, s.length() - 1);
		return s;
	}

	private int totalInFlightGCodeSize() {
		return inFlightGCodes.stream().collect(Collectors.summingInt(x -> x.gCode.length));
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
			statusPending.tryAcquire(2, TimeUnit.SECONDS);
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

}
