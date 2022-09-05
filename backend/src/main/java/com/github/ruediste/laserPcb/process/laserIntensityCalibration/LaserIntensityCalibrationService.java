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

	public List<Integer> getLaserIntensities(LaserIntensityCalibrationProcess process) {
		if (process.count < 2)
			return null;
		List<Integer> result = new ArrayList<>();
		double f = Math.pow(process.maxIntensity / (double) process.minIntensity, 1. / (process.count - 1));
		int intensity = process.minIntensity;
		for (int i = 0; i < process.count; i++) {
			result.add(intensity);

			// calculate next step
			int newIntensity = (int) (process.minIntensity * Math.pow(f, i + 1));
			if (newIntensity <= intensity) // make sure intensity always increases
				newIntensity = intensity + 1;

			// check if we reached the max intensity
			if (newIntensity >= process.maxIntensity) {

				// add the max intensity if it has not been added before
				if (intensity < process.maxIntensity && result.size() < process.count)
					result.add(process.maxIntensity);
				break;
			}
			intensity = newIntensity;
		}
		return result;
	}

	public GCodeWriter buildPatternGCode(LaserIntensityCalibrationProcess process) {

		Profile profile = profileRepo.getCurrent();

		var gCode = new GCodeWriter();
		gCode.splitAndAdd(profile.preExposeGCode);
		gCode.add("G21"); // set units to millimeters
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);

		gCode.absolutePositioning();
		gCode.g0(null, null, profile.laserZ); // move to laser z

		gCode.relativePositioning();
		gCode.g0(-profile.cameraOffsetX, -profile.cameraOffsetY); // move by the current camera offset

		boolean first = true;
		var laserIntensities = getLaserIntensities(process);
		for (var laserIntensity : laserIntensities) {
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
