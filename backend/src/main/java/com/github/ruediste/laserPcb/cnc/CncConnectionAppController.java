package com.github.ruediste.laserPcb.cnc;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

@Component
public class CncConnectionAppController {

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private CncConnection connection;
	private ScheduledFuture<?> stateFuture;

	private boolean isJogging;

	public synchronized void connect(Path serialDev) {
		disconnect();
		connection = new CncConnection(new SerialConnection(serialDev.toAbsolutePath().toString()));
		stateFuture = executor.scheduleWithFixedDelay(() -> connection.queryStatus(), 500, 500, TimeUnit.MILLISECONDS);
		isJogging = false;
	}

	public synchronized void disconnect() {

		if (connection != null) {
			stateFuture.cancel(true);
			connection.close();
			connection = null;
		}
	}

	public synchronized CncConnection getConnection() {
		return connection;
	}

	@PreDestroy
	public void preDestroy() {
		disconnect();
		executor.shutdownNow();
	}

	public void ensureJogging() {
		if (!isJogging) {
			isJogging = true;
			connection.send("G92\n");
			connection.send("G21\n");
			connection.send("G0 F100\n");
		}
	}

}
