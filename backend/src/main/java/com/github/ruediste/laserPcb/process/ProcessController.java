package com.github.ruediste.laserPcb.process;

import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcess.CameraCalibrationStep;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;

@Service
public class ProcessController {
	private final Logger log = LoggerFactory.getLogger(ProcessController.class);

	@Autowired
	ProcessRepository repo;

	private Process process;
	private Object lock = new Object();

	@PostConstruct
	void init() {
		try {
			process = repo.get();
		} catch (Exception e) {
			log.error("Error while loading process. Creating fresh instance.", e);
			process = null;
		}
		if (process == null) {
			log.info("No persisted process found during initialization, creating new process");
			process = new Process();
			process.printPcb = new PrintPcbProcess();
			repo.set(process);
		}

		if (process.cameraCalibration != null) {
			process.cameraCalibration.currentStep = CameraCalibrationStep.MOVE_TO_ORIGIN;
		}
		repo.set(process);
	}

	/**
	 * return a copy of the current process
	 */
	public Process get() {
		return repo.get();
	}

	public void update(Consumer<Process> updater) {
		synchronized (lock) {
			updater.accept(process);
			repo.set(process);
		}
	}

}
