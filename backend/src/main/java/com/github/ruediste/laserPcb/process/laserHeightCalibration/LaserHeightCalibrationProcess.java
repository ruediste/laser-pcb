package com.github.ruediste.laserPcb.process.laserHeightCalibration;

public class LaserHeightCalibrationProcess {
	public enum LaserHeightCalibrationStep {
		/**
		 * Prepare the Exposure: move laser to origin, set parameters
		 */
		PREPARE,

		/**
		 * Expose the pattern
		 */
		EXPOSE_PATTERN,

		/**
		 * Set the laser height based on the result of the pattern
		 */
		SET_HEIGHT
	}

	public LaserHeightCalibrationStep currentStep = LaserHeightCalibrationStep.PREPARE;

	public double startHeight;
	public double endHeight;
	public int count;

}
