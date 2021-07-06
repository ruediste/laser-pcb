package com.github.ruediste.laserPcb.cnc;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CncConnectionAppController {
	private final Logger log = LoggerFactory.getLogger(CncConnectionAppController.class);

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private CncConnection connection;
	private ScheduledFuture<?> stateFuture;

	private volatile boolean statusQueryEnabled = true;

	public synchronized void connect(Path serialDev, int baudRate) {
		disconnect();
		connection = new CncConnection(new SerialConnection(serialDev.toAbsolutePath().toString(), baudRate));
		stateFuture = executor.scheduleWithFixedDelay(() -> {
			if (statusQueryEnabled)
				connection.queryStatus();
		}, 2000, 1000, TimeUnit.MILLISECONDS);
	}

	public void setStatusQueryEnabled(boolean enabled) {
		statusQueryEnabled = enabled;
	}

	public synchronized void disconnect() {
		if (connection != null) {
			log.info("Disconnecting from {}", connection.con.port);
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

}
