package com.github.ruediste.laserPcb.cnc;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Watches /dev/* for changes
 */
@Component
public class AvailableDevicesAppController {

	private final Logger log = LoggerFactory.getLogger(AvailableDevicesAppController.class);

	private WatchService watchService;
	private Path directory = Paths.get("/dev");

	@PostConstruct
	public void postConstruct() throws IOException {
		watchService = FileSystems.getDefault().newWatchService();
		new Thread(this::watch, "availaleDevicesScanner").start();
	}

	@PreDestroy
	public void preDestroy() {
		try {
			watchService.close();
		} catch (IOException e) {
			log.error("Error while closing watch service", e);
		}
	}

	private final Set<Path> currentSerialConnections = new HashSet<>();
	private final Set<Path> currentVideoConnections = new HashSet<>();

	private void watch() {
		try {
			directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE);
			scan();
			WatchKey key;
			while ((key = watchService.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					log.debug("{} event happened on {}", event.kind(), event.context());
					if (event.kind() == ENTRY_CREATE) {
						synchronized (this) {
							Path file = directory.resolve((Path) event.context()).toAbsolutePath();
							if (isSerialConnection(file)) {
								log.info("Adding serial connection {}", file);
								currentSerialConnections.add(file);
							}
							if (isVideoConnection(file)) {
								log.info("Adding video connection {}", file);
								currentVideoConnections.add(file);
							}
						}
					}
					if (event.kind() == ENTRY_DELETE) {
						synchronized (this) {
							Path file = directory.resolve((Path) event.context()).toAbsolutePath();
							if (isSerialConnection(file)) {
								log.info("Removing serial connection {}", file);
								currentSerialConnections.remove(file);
							}
							if (isVideoConnection(file)) {
								log.info("Removing video connection {}", file);
								currentVideoConnections.remove(file);
							}
						}
					}
					if (event.kind() == OVERFLOW) {
						scan();
					}

				}

				key.reset();
			}
		} catch (ClosedWatchServiceException e) {
			// NOP
		} catch (Exception e) {
			log.error("Error while watching directory", e);
		}

	}

	private boolean isSerialConnection(Path path) {
		return path.getFileName().toString().startsWith("ttyUSB");
	}

	private boolean isVideoConnection(Path path) {
		return path.getFileName().toString().startsWith("video");
	}

	private synchronized void scan() {
		log.info("Device scan of {} starting ...", directory.toAbsolutePath());
		currentSerialConnections.clear();
		currentVideoConnections.clear();
		try {
			Files.list(directory).forEach(file -> {
				if (isSerialConnection(file)) {
					log.info("Adding serial connection {}", file.toAbsolutePath());
					currentSerialConnections.add(file.toAbsolutePath());
				}
				if (isVideoConnection(file)) {
					log.info("Adding video connection {}", file.toAbsolutePath());
					currentVideoConnections.add(file.toAbsolutePath());
				}
			});

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Serial connection scan of {} complete", directory.toAbsolutePath());
	}

	public synchronized Set<Path> getCurrentSerialConnections() {
		return new HashSet<>(currentSerialConnections);
	}

	public synchronized Set<Path> getCurrentVideoConnections() {
		return new HashSet<>(currentVideoConnections);
	}
}
