package com.github.ruediste.laserPcb.process.laserHeightCalibration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class LaserHeightCalibrationServiceTest {

	@Test
	public void testGetLaserHeights() throws Exception {
		LaserHeightCalibrationService service = new LaserHeightCalibrationService();
		LaserHeightCalibrationProcess process = new LaserHeightCalibrationProcess();
		process.startHeight = 10;
		process.endHeight = 12;
		process.count = 5;
		assertEquals(List.of(10., 10.5, 11., 11.5, 12.), service.getLaserHeights(process));
	}

}
