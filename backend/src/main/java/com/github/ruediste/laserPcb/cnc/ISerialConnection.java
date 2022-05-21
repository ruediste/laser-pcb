package com.github.ruediste.laserPcb.cnc;

import java.io.InputStream;

/**
 * Represents the raw serial connection to a CNC Machine. Possibly implemented
 * by a simulator
 */
public interface ISerialConnection {

	void close();

	void sendBytes(byte[] tx);

	String getPort();

	InputStream getIn();

}