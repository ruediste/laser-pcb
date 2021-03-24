package com.github.ruediste.laserPcb.process.laserCalibration;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.Var;
import com.github.ruediste.laserPcb.cnc.CncConnection;
import com.github.ruediste.laserPcb.cnc.CncConnectionAppController;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.process.ProcessAppController;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
@Scope("singleton")
public class LaserCalibrationAppController {
	private final Logger log = LoggerFactory.getLogger(LaserCalibrationAppController.class);

	@Autowired
	CncConnectionAppController connCtrl;

	@Autowired
	ProcessAppController processAppCtrl;

	@Autowired
	ProfileRepository profileRepo;

	private String formatFeed(double value) {
		return "" + (int) Math.round(value);
	}

	private String formatCoordinate(double value) {
		return String.format("%.2f", value);
	}

	public void printPattern() {
		log.info("Print Pattern");
		Profile profile = profileRepo.getCurrent();
		GCodeWriter gCodes = new GCodeWriter();
		LaserCalibrationProcess process = processAppCtrl.get().laserCalibration;
		gCodes.add("G90"); // absolute positioning
		gCodes.add("G21"); // set units to millimeters
		gCodes.g0(null, null, profile.laserZ, 10000.); // go to laser hight

		double lineLength = 10;
		double lineDistance = 10;

		double originX = profile.bedSizeX / 2 - lineLength / 2;
		double originY = profile.bedSizeY / 2 - lineDistance / 2;
		gCodes.g0(originX, originY);
		gCodes.add(profile.laserOn);
		gCodes.g1(originX + lineLength, null, null, process.v1);
		gCodes.add(profile.laserOff);
		gCodes.g0(originX, originY + lineDistance);
		gCodes.add(profile.laserOn);
		gCodes.g1(originX + lineLength, null, null, process.v2);
		gCodes.add(profile.laserOff);

		CncConnection conn = connCtrl.getConnection();

		var it = gCodes.getGCodes().listIterator();
		if (!conn.sendGCodeNonBlocking(it.next(), null))
			it.previous();

		Var<Consumer<Void>> sender = Var.of();

		sender.value = x -> {
			synchronized (gCodes) {
				while (it.hasNext()) {
					if (!conn.sendGCodeNonBlocking(it.next(), null)) {
						it.previous();
						break;
					}
				}
				if (!it.hasNext()) {
					conn.gCodeCompleted.remove(sender.get());
				}
			}
		};

		conn.gCodeCompleted.add(sender.get());
		sender.get().accept(null);
	}
}
