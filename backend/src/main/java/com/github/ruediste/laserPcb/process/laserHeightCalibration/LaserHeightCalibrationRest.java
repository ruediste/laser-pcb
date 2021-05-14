package com.github.ruediste.laserPcb.process.laserHeightCalibration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.cnc.CncConnectionAppController;
import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationProcess.LaserHeightCalibrationStep;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@RestController
public class LaserHeightCalibrationRest {
	private final Logger log = LoggerFactory.getLogger(LaserHeightCalibrationRest.class);

	@Autowired
	ProcessController processAppController;

	@Autowired
	LaserHeightCalibrationService service;

	@Autowired
	SendGCodeController ctrl;

	@Autowired
	CncConnectionAppController connCtrl;

	@Autowired
	ProfileRepository profileRepo;

	@PostMapping("process/laserHeightCalibration/_start")
	void start() {
		Profile profile = profileRepo.getCurrent();
		var calibration = new LaserHeightCalibrationProcess();
		calibration.startHeight = profile.laserZ - 2;
		calibration.endHeight = profile.laserZ + 2;
		calibration.count = 5;
		processAppController.update(p -> p.clear().laserHeightCalibration = calibration);
	}

	@GetMapping("process/laserHeightCalibration")
	LaserHeightCalibrationProcess readProcess() {
		return processAppController.get().laserHeightCalibration;
	}

	@PostMapping("process/laserHeightCalibration")
	void update(@RequestBody LaserHeightCalibrationProcess updated) {
		processAppController.update(p -> p.laserHeightCalibration = updated);
	}

	@PostMapping("process/laserHeightCalibration/_exposePattern")
	void exposePattern() {
		GCodeWriter gCodes = service.buildPatternGCode(processAppController.get().laserHeightCalibration);
		gCodes.dumpToDebugFile();

		updateStep(LaserHeightCalibrationStep.EXPOSE_PATTERN);

		ctrl.sendGCodes(gCodes).thenRun(() -> updateStep(LaserHeightCalibrationStep.SET_HEIGHT)).exceptionally(t -> {
			log.error("Error while exposing pattern", t);
			updateStep(LaserHeightCalibrationStep.PREPARE);
			return null;
		});
	}

	private void updateStep(LaserHeightCalibrationStep step) {
		processAppController.update(p -> p.laserHeightCalibration.currentStep = step);
	}

	@PostMapping("process/laserHeightCalibration/_setHeight")
	void _setHeight(@RequestParam Double laserHeight) {
		Profile profile = profileRepo.getCurrent();
		profile.laserZ = laserHeight;
		profileRepo.save(profile);
		processAppController.update(p -> p.laserHeightCalibration = null);
	}
}
