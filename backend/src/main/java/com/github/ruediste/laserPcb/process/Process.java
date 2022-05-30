package com.github.ruediste.laserPcb.process;

import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcess;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationProcess;
import com.github.ruediste.laserPcb.process.laserIntensityCalibration.LaserIntensityCalibrationProcess;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;

public class Process {
	public PrintPcbProcess printPcb;
	public CameraCalibrationProcess cameraCalibration;
	public LaserHeightCalibrationProcess laserHeightCalibration;
	public LaserIntensityCalibrationProcess laserIntensityCalibration;

	public Process clear() {
		printPcb = null;
		cameraCalibration = null;
		laserHeightCalibration = null;
		laserIntensityCalibration = null;
		return this;
	}
}
