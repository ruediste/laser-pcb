package com.github.ruediste.laserPcb.process.laserIntensityCalibration;

public class LaserIntensityCalibrationProcess {
	public enum LaserIntensityCalibrationStep {
		/**
		 * Prepare the Exposure: move laser to origin, set parameters
		 */
		PREPARE,

		/**
		 * Expose the pattern
		 */
		EXPOSE_PATTERN,

		/**
		 * Set the laser intensity based on the result of the pattern
		 */
		SET_INTENSITY
	}

	public LaserIntensityCalibrationStep currentStep = LaserIntensityCalibrationStep.PREPARE;

	public int minIntensity;
	public int maxIntensity;
	public int count;

}
