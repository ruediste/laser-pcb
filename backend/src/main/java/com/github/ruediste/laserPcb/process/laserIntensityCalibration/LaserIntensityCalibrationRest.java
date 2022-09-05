package com.github.ruediste.laserPcb.process.laserIntensityCalibration;

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
import com.github.ruediste.laserPcb.process.laserIntensityCalibration.LaserIntensityCalibrationProcess.LaserIntensityCalibrationStep;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@RestController
public class LaserIntensityCalibrationRest {
	private final Logger log = LoggerFactory.getLogger(LaserIntensityCalibrationRest.class);

	@Autowired
	ProcessController processAppController;

	@Autowired
	LaserIntensityCalibrationService service;

	@Autowired
	SendGCodeController ctrl;

	@Autowired
	CncConnectionAppController connCtrl;

	@Autowired
	ProfileRepository profileRepo;

	@PostMapping("process/laserIntensityCalibration/_start")
	void start() {
		Profile profile = profileRepo.getCurrent();
		var calibration = new LaserIntensityCalibrationProcess();
		calibration.maxIntensity = (int) Math.min(255, profile.laserIntensity * 1.5);
		calibration.minIntensity = (int) (calibration.maxIntensity / 1.5 / 1.5);
		calibration.count = 5;
		processAppController.update(p -> p.clear().laserIntensityCalibration = calibration);
	}

	@GetMapping("process/laserIntensityCalibration")
	LaserIntensityCalibrationProcess readProcess() {
		return processAppController.get().laserIntensityCalibration;
	}

	@PostMapping("process/laserIntensityCalibration")
	void update(@RequestBody LaserIntensityCalibrationProcess updated) {
		processAppController.update(p -> p.laserIntensityCalibration = updated);
	}

	@PostMapping("process/laserIntensityCalibration/_exposePattern")
	void exposePattern() {
		GCodeWriter gCodes = service.buildPatternGCode(processAppController.get().laserIntensityCalibration);
		gCodes.dumpToDebugFile();

		updateStep(LaserIntensityCalibrationStep.EXPOSE_PATTERN);

		ctrl.sendGCodes(gCodes).thenRun(() -> updateStep(LaserIntensityCalibrationStep.SET_INTENSITY))
				.exceptionally(t -> {
					log.error("Error while exposing pattern", t);
					updateStep(LaserIntensityCalibrationStep.PREPARE);
					return null;
				});
	}

	private void updateStep(LaserIntensityCalibrationStep step) {
		processAppController.update(p -> p.laserIntensityCalibration.currentStep = step);
	}

	@PostMapping("process/laserIntensityCalibration/_setIntensity")
	void setIntensity(@RequestParam int laserIntensity) {
		Profile profile = profileRepo.getCurrent();
		profile.laserIntensity = laserIntensity;
		profileRepo.save(profile);
		processAppController.update(p -> p.laserIntensityCalibration = null);
	}
}
