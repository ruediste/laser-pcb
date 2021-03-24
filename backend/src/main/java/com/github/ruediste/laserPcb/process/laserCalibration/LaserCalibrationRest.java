package com.github.ruediste.laserPcb.process.laserCalibration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.process.ProcessAppController;

@RestController
public class LaserCalibrationRest {
	private final Logger log = LoggerFactory.getLogger(LaserCalibrationRest.class);

	@Autowired
	ProcessAppController processAppController;

	@Autowired
	LaserCalibrationAppController ctrl;

	@PostMapping("process/laserCalibration/start")
	void start(@RequestParam double v1, @RequestParam double v2) {
		processAppController.update(p -> p.clear().laserCalibration = new LaserCalibrationProcess(v1, v2));
	}

	@PostMapping("process/laserCalibration/printPattern")
	void printPattern() {
		ctrl.printPattern();
	}
}
