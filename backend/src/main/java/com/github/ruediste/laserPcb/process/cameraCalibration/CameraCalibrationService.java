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
		gCode.splitAndAdd(profile.preExposeGCode);

		gCode.unitsMM().absolutePositioning();
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);
		gCode.g0(null, null, profile.laserZ);

		gCode.relativePositioning();

		gCode.g0(profile.cameraOffsetX, profile.cameraOffsetY); // move by the current camera offset

		gCode.laserOn(profile);
		gCode.g1(10., null);
		gCode.laserOff(profile);

		gCode.g0(-5., 5.);

		gCode.laserOn(profile);
		gCode.g1(null, -10.);
		gCode.add("G2 Y10 J5");
		gCode.add("G2 Y-10 J-5");
		gCode.laserOff(profile);

		gCode.g0(-profile.cameraOffsetX - 5, -profile.cameraOffsetY + 5); // move camera back over cross just exposed
																			// (if the offset was already correct)
		gCode.absolutePositioning();
		gCode.g0(profile.cameraZ);
		return gCode;
	}
}
