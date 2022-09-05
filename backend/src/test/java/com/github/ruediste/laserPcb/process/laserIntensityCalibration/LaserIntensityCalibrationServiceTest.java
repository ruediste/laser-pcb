package com.github.ruediste.laserPcb.process.laserIntensityCalibration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LaserIntensityCalibrationServiceTest {

	private LaserIntensityCalibrationService service;

	@BeforeEach
	public void before() {
		service = new LaserIntensityCalibrationService();
	}

	@Test
	public void testGetLaserIntensities() throws Exception {
		LaserIntensityCalibrationProcess process = new LaserIntensityCalibrationProcess();
		process.count = 5;
		process.minIntensity = 50;
		process.maxIntensity = 300;
		assertEquals(List.of(50, 78, 122, 191, 300), service.getLaserIntensities(process));

		process.minIntensity = 35;
		process.maxIntensity = 40;
		assertEquals(List.of(35, 36, 37, 38, 40), service.getLaserIntensities(process));

		process.minIntensity = 35;
		process.maxIntensity = 39;
		assertEquals(List.of(35, 36, 37, 38, 39), service.getLaserIntensities(process));

		process.minIntensity = 35;
		process.maxIntensity = 37;
		assertEquals(List.of(35, 36, 37), service.getLaserIntensities(process));
	}

}
