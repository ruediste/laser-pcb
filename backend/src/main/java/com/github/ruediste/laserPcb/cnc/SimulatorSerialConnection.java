package com.github.ruediste.laserPcb.cnc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connection to a simulated GRBL controller
 */
public class SimulatorSerialConnection implements ISerialConnection {
	private final Logger log = LoggerFactory.getLogger(SimulatorSerialConnection.class);

	private OutputStreamWriter writeOut;
	private PipedOutputStream readOut = new PipedOutputStream();
	private PipedInputStream writeIn;
	private BufferedReader readIn;

	private volatile boolean closing;
	private CountDownLatch closed = new CountDownLatch(1);

	public SimulatorSerialConnection() {
		try {
			var writeOut = new PipedOutputStream();
			this.writeOut = new OutputStreamWriter(writeOut, StandardCharsets.UTF_8);
			writeIn = new PipedInputStream(writeOut);
			readIn = new BufferedReader(new InputStreamReader(new PipedInputStream(readOut), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		new Thread(this::loop, "simulator").start();
	}

	private boolean incrementalPositioning;
	private double xPos;
	private double yPos;
	private double zPos;

	private void loop() {
		log.info("Starting Simulator");
		try {
			writeLine("Grbl");
			while (true) {
				String line = readIn.readLine();
				if (line.equals("?")) {
					writeLine(String.format("<Idle|MPos:%.2f,%.2f,%.2f|WPos:0.000,0.000,0.000,Pin:000|0|0000>", xPos,
							yPos, zPos));
				} else {
					var parts = line.toUpperCase(Locale.ENGLISH).split(" ");
					if (parts.length > 0) {
						switch (parts[0]) {
						case "G0":
						case "G1": {
							for (int i = 1; i < parts.length; i++) {
								if (parts[i].startsWith("X")) {
									xPos = (incrementalPositioning ? xPos : 0)
											+ Double.parseDouble(parts[i].substring(1));
								}
								if (parts[i].startsWith("Y")) {
									yPos = (incrementalPositioning ? yPos : 0)
											+ Double.parseDouble(parts[i].substring(1));
								}
								if (parts[i].startsWith("Z")) {
									zPos = (incrementalPositioning ? zPos : 0)
											+ Double.parseDouble(parts[i].substring(1));
								}
							}
						}
							break;
						case "G28":
							xPos = 0;
							yPos = 0;
							zPos = 0;
							break;
						case "G90":
							incrementalPositioning = false;
							break;
						case "G91":
							incrementalPositioning = true;
							break;
						case "G21":
							// metric units, nop
							break;
						case "M201":
							// acceleration, nop
							break;
						case "M106":
							// set fan speed
							break;
						case "M107":
							// fan off
							break;
						default:
							log.info("Received unhandled line {}", line);
							break;
						}
					}
					writeLine("ok");
				}
				if (closing) {
					break;
				}
			}
		} catch (Throwable t) {
			log.error("Error in simulator", t);
		}
		closed.countDown();
		log.info("simulator stopped");
	}

	private void writeLine(String line) throws IOException {
		writeOut.append(line + "\n");
		writeOut.flush();
	}

	@Override
	public void close() {
		closing = true;
		try {
			readOut.close();
			writeOut.close();
			readIn.close();
			writeIn.close();
			closed.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void sendBytes(byte[] tx) {
		try {
			readOut.write(tx);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getPort() {
		return "simulator";
	}

	@Override
	public InputStream getIn() {
		return writeIn;
	}

}
