package com.github.ruediste.laserPcb.process.laserCalibration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class LaserCalibrationPatternService {
	private final Logger log = LoggerFactory.getLogger(LaserCalibrationPatternService.class);

	@Autowired
	SendGCodeController sendController;

	@Autowired
	ProcessController processAppCtrl;

	@Autowired
	ProfileRepository profileRepo;

	public void printPattern() {
		Profile profile = profileRepo.getCurrent();
		LaserCalibrationProcess process = processAppCtrl.get().laserCalibration;

		log.info("Print Pattern");
		GCodeWriter gCodes = new GCodeWriter();
		gCodes.add("G90"); // absolute positioning
		gCodes.add("G21"); // set units to millimeters
		double fastFeed = 10000.;
		gCodes.g0(null, null, profile.laserZ, fastFeed); // go to laser height

		double lineLength = 10;
		double lineDistance = 10;

		double originX = profile.bedSizeX / 2 - lineLength / 2;
		double originY = profile.bedSizeY / 2 - lineDistance / 2;
		gCodes.g0(originX, originY, null, fastFeed);
		gCodes.add(profile.laserOn);
		gCodes.g1(originX + lineLength, null, null, process.v1);
		gCodes.add(profile.laserOff);
		gCodes.g0(originX, originY + lineDistance, null, fastFeed);
		gCodes.add(profile.laserOn);
		gCodes.g1(originX + lineLength, null, null, process.v2);
		gCodes.add(profile.laserOff);

		sendController.sendGCodes(gCodes);
	}
}
