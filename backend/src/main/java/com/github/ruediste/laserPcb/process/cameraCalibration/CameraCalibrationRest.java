package com.github.ruediste.laserPcb.process.cameraCalibration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.cnc.CncConnection.CncState;
import com.github.ruediste.laserPcb.cnc.CncConnectionAppController;
import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcess.CameraCalibrationStep;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@RestController
public class CameraCalibrationRest {
	private final Logger log = LoggerFactory.getLogger(CameraCalibrationRest.class);

	@Autowired
	ProcessController processAppController;

	@Autowired
	CameraCalibrationService service;

	@Autowired
	SendGCodeController ctrl;

	@Autowired
	CncConnectionAppController connCtrl;

	@Autowired
	ProfileRepository profileRepo;

	@PostMapping("process/cameraCalibration/start")
	void start() {
		processAppController.update(p -> p.clear().cameraCalibration = new CameraCalibrationProcess());
	}

	@PostMapping("process/cameraCalibration/exposeCross")
	void exposeCross() {
		CncState state = connCtrl.getConnection().getState();
		processAppController.update(p -> {
			CameraCalibrationProcess cameraCalibration = p.cameraCalibration;
			cameraCalibration.currentStep = CameraCalibrationStep.EXPOSE_CROSS;
			cameraCalibration.crossX = state.x + 5;
			cameraCalibration.crossY = state.y;
		});
		ctrl.sendGCodes(service.buildExposeCrossGCode()).thenRun(() -> processAppController
				.update(p -> p.cameraCalibration.currentStep = CameraCalibrationStep.POSITION_CAMERA));
	}

	@PostMapping("process/cameraCalibration/applyOffset")
	void applyOffset() {
		CncState state = connCtrl.getConnection().getState();
		Profile profile = profileRepo.getCurrent();
		CameraCalibrationProcess process = processAppController.get().cameraCalibration;
		profile.cameraOffsetX = state.x - process.crossX;
		profile.cameraOffsetY = state.y - process.crossY;
		log.info("Camera Offset: {}/{} crossExposurePosition: {}/{} cameraPosition: {}/{}", profile.cameraOffsetX,
				profile.cameraOffsetY, process.crossX, process.crossY, state.x, state.y);
		profileRepo.save(profile);
		processAppController.update(p -> p.cameraCalibration = null);
	}
}
