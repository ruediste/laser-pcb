package com.github.ruediste.laserPcb.process.laserCalibration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.ruediste.laserPcb.process.laserCalibration.CalibrationService;
import com.github.ruediste.laserPcb.process.laserCalibration.CalibrationService.CalibrationMeasurement;
import com.github.ruediste.laserPcb.process.laserCalibration.CalibrationService.LaserParameters;

public class CalibrationServiceTest {
	@Test
	public void test() {
		CalibrationService service = new CalibrationService();
		LaserParameters params = service
				.calculateParameters(List.of(new CalibrationMeasurement(1, 0.4), new CalibrationMeasurement(2, 0.35)));
//		System.out.println(params);
		CalibrationService.ToolConfig cfg = new CalibrationService.ToolConfig();
		cfg.params = params;
		cfg.v = 1;
//		System.out.println("v: " + cfg.v + " w: " + cfg.lineWidth());
		cfg.v = 2;
//		System.out.println("v: " + cfg.v + " w: " + cfg.lineWidth());
//		System.out.println();
		assertEquals(0.41, params.d, 0.01);
		assertEquals(1.21, params.P, 0.01);
	}

	@Test
	public void testGoodDifference() {
		CalibrationService service = new CalibrationService();
		LaserParameters params = service
				.calculateParameters(List.of(new CalibrationMeasurement(1, 0.4), new CalibrationMeasurement(2, 0.18)));
		assertEquals(0.45, params.d, 0.01);
		assertEquals(0.77, params.P, 0.01);
	}
}
