package com.github.ruediste.laserPcb.process.cameraCalibration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class CameraCalibrationService {

	@Autowired
	SendGCodeController ctrl;

	@Autowired
	ProfileRepository profileRepo;

	public GCodeWriter buildExposeCrossGCode() {
		Profile profile = profileRepo.getCurrent();

		var gCode = new GCodeWriter();
		gCode.add("G90"); // absolute positioning
		gCode.add("G21"); // set units to millimeters
		gCode.add("G92 X0 Y0 Z0"); // set local coordinate system
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);

		gCode.add(profile.laserOn);
		gCode.g1(10., null);
		gCode.add(profile.laserOff);

		gCode.g0(5., 5.);

		gCode.add(profile.laserOn);
		gCode.g1(null, -5.);
		gCode.add("G2 Y5 J5");
		gCode.add("G2 Y-5 J-5");
		gCode.add(profile.laserOff);

		gCode.add("G92.1"); // switch back to machine coordinates
		return gCode;
	}
}
