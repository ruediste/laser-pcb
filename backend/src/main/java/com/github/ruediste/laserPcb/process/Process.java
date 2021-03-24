package com.github.ruediste.laserPcb.process;

import com.github.ruediste.laserPcb.process.laserCalibration.LaserCalibrationProcess;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;

public class Process {
	public PrintPcbProcess printPcb;
	public LaserCalibrationProcess laserCalibration;

	public Process clear() {
		printPcb = null;
		laserCalibration = null;
		return this;
	}
}
