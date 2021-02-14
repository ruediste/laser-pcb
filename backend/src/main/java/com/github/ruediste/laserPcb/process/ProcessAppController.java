package com.github.ruediste.laserPcb.process;

import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;

@Service
@Scope("singleton")
public class ProcessAppController {
	private final Logger log = LoggerFactory.getLogger(ProcessAppController.class);

	@Autowired
	ProcessRepository repo;

	private Process process;
	private Object lock = new Object();

	@PostConstruct
	void init() {
		process = repo.get();
		if (process == null) {
			log.info("No persisted process found during initialization, creating new process");
			process = new Process();
			process.printPcb = new PrintPcbProcess();
			repo.set(process);
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
