package com.github.ruediste.laserPcb.process.laserIntensityCalibration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class LaserIntensityCalibrationService {

	@Autowired
	ProfileRepository profileRepo;

	public List<Double> getLaserIntensities(LaserIntensityCalibrationProcess process) {
		if (process.count < 2)
			return null;
		List<Double> result = new ArrayList<>();
		double f = Math.pow(process.maxIntensity / process.minIntensity, 1. / (process.count - 1));
		for (int i = 0; i < process.count; i++) {
			result.add(Math.round(1000 * process.minIntensity * Math.pow(f, i)) / 1000.);
		}
		return result;
	}

	public GCodeWriter buildPatternGCode(LaserIntensityCalibrationProcess process) {

		Profile profile = profileRepo.getCurrent();

		var gCode = new GCodeWriter();
		gCode.add("G21"); // set units to millimeters
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);

		gCode.absolutePositioning();
		gCode.g0(null, null, profile.laserZ); // move to laser z

		gCode.relativePositioning();
		gCode.g0(profile.cameraOffsetX, profile.cameraOffsetY); // move by the current camera offset

		boolean first = true;
		List<Double> laserIntensities = getLaserIntensities(process);
		for (double laserIntensity : laserIntensities) {
			if (!first)
				gCode.g0(null, profile.exposureWidth * 5); // leave a gap between exposure pairs
			first = false;

			// expose pair
			gCode.laserOn(profile, laserIntensity);
			gCode.g1(50., null);
			gCode.g0(null, profile.exposureWidth);
			gCode.g1(-50., null);
			gCode.laserOff(profile);
		}

		return gCode;
	}
}
