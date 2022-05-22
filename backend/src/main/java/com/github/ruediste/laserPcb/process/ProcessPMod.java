package com.github.ruediste.laserPcb.process;

import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcessPMod;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationProcessPMod;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessPMod;

public class ProcessPMod {
	public PrintPcbProcessPMod printPcb;
	public CameraCalibrationProcessPMod cameraCalibration;
	public LaserHeightCalibrationProcessPMod laserHeightCalibration;
}
