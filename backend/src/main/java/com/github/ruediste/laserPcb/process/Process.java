package com.github.ruediste.laserPcb.process;

import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcess;
import com.github.ruediste.laserPcb.process.laserCalibration.LaserCalibrationProcess;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;

public class Process {
	public PrintPcbProcess printPcb;
	public LaserCalibrationProcess laserCalibration;
	public CameraCalibrationProcess cameraCalibration;

	public Process clear() {
		printPcb = null;
		laserCalibration = null;
		cameraCalibration = null;
		return this;
	}
}
