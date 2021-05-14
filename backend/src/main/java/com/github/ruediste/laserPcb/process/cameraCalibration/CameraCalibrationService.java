package com.github.ruediste.laserPcb.process.cameraCalibration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class CameraCalibrationService {

	@Autowired
	ProfileRepository profileRepo;

	public GCodeWriter buildExposeCrossGCode() {
		Profile profile = profileRepo.getCurrent();

		var gCode = new GCodeWriter();
		gCode.add("G91"); // relative positioning
		gCode.add("G21"); // set units to millimeters
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);

		gCode.add(profile.laserOn);
		gCode.g1(10., null);
		gCode.add(profile.laserOff);

		gCode.g0(-5., 5.);

		gCode.add(profile.laserOn);
		gCode.g1(null, -10.);
		gCode.add("G2 Y10 J5");
		gCode.add("G2 Y-10 J-5");
		gCode.add(profile.laserOff);
		return gCode;
	}
}
