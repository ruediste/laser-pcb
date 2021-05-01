package com.github.ruediste.laserPcb.process.cameraCalibration;

/**
 * The camera calibration process consists of the following steps:
 * 
 * <ol>
 * <li>Show jogging and laser controls, ask user to move to the left center of a
 * cross to be exposed (10 by 10 mm, with a 5mm diameter circle)</li>
 * <li>expose the cross</li>
 * <li>Show jogging controls and camera, ask user to align the camera to the
 * center of the exposed cross</li>
 * <li>Save X and Y offset</li>
 * </ol>
 */
public class CameraCalibrationProcess {

	public enum CameraCalibrationStep {
		MOVE_TO_ORIGIN, EXPOSE_CROSS, POSITION_CAMERA
	}

	public CameraCalibrationStep currentStep = CameraCalibrationStep.MOVE_TO_ORIGIN;

	public Double crossX;
	public Double crossY;

	public CameraCalibrationProcess() {
	}

}
