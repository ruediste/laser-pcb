package com.github.ruediste.laserPcb.process.laserHeightCalibration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class LaserHeightCalibrationService {

	@Autowired
	ProfileRepository profileRepo;

	public List<Double> getLaserHeights(LaserHeightCalibrationProcess process) {
		if (process.count < 2)
			return null;
		List<Double> result = new ArrayList<>();
		for (int i = 0; i < process.count; i++) {
			result.add(process.startHeight + i * (process.endHeight - process.startHeight) / (process.count - 1));
		}
		return result;
	}

	public GCodeWriter buildPatternGCode(LaserHeightCalibrationProcess process) {

		Profile profile = profileRepo.getCurrent();

		var gCode = new GCodeWriter();
		gCode.add("G21"); // set units to millimeters
		gCode.g0(profile.fastMovementFeed);
		gCode.g1(profile.exposureFeed);

		gCode.relativePositioning();
		gCode.g0(profile.cameraOffsetX, profile.cameraOffsetY); // move by the current camera offset

		boolean first = true;
		List<Double> laserHeights = getLaserHeights(process);
		for (double z : laserHeights) {
			gCode.absolutePositioning();
			gCode.g0(null, null, z); // go to laser height

			gCode.relativePositioning();
			if (!first)
				gCode.g0(null, profile.exposureWidth * 5); // leave a gap between exposure pairs
			first = false;

			// expose pair
			gCode.laserOn(profile);
			gCode.g1(50., null);
			gCode.g0(null, profile.exposureWidth);
			gCode.g1(-50., null);
			gCode.laserOff(profile);
		}

		return gCode;
	}
}
