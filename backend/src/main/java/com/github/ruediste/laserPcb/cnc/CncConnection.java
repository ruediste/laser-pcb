package com.github.ruediste.laserPcb.cnc;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CncConnection {
	private final Logger log = LoggerFactory.getLogger(CncConnection.class);

	private SerialConnection con;
	private CncState state = new CncState();
	private volatile boolean closing;
	private CountDownLatch closed = new CountDownLatch(1);

	public CncConnection(SerialConnection con) {
		this.con = con;
		new Thread(this::readLoop, "serialReader " + con.port).start();
	}

	static Pattern statusLinePattern = Pattern.compile(
			"<(?<status>[A-Z][a-z]+)\\|MPos:(?<x>\\d+\\.\\d+),(?<y>\\d+\\.\\d+),(?<z>\\d+\\.\\d+)(\\|[^>]*)*>");

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
					log.info("received line {}", line);
					Matcher matcher = statusLinePattern.matcher(line);
					if (matcher.matches()) {
						log.info("Status received");
						if (statusPending.availablePermits() == 0)
							statusPending.release();
						synchronized (state) {
							state.x = Double.parseDouble(matcher.group("x"));
							state.y = Double.parseDouble(matcher.group("y"));
							state.z = Double.parseDouble(matcher.group("z"));
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

	public void send(String gCode) {
		log.info("Sending {}", gCode);
		byte[] bb = gCode.getBytes(StandardCharsets.UTF_8);
		con.sendBytes(bb);
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
		try {
			statusPending.tryAcquire(2, TimeUnit.SECONDS);
			// after two seconds just ignore the locked semaphore
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		send("?");
	}

	public CncState getState() {
		synchronized (state) {
			return new CncState(state);
		}
	}

}
