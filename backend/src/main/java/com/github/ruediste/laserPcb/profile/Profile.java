package com.github.ruediste.laserPcb.profile;

import com.github.ruediste.laserPcb.EntityBase;

public class Profile extends EntityBase {

	public String name;
	public boolean singleLayerPcb;

	public double laserPower;
	public double laserDotSize;
	public double laserZ;

	public int cameraRotation;

	public int baudRate;

	/**
	 * overlap between exposures (fraction of exposure width)
	 */
	public double exposureOverlap;

	/**
	 * Temporary setting manually configure exposure
	 */
	public double exposureWidth;

	/**
	 * Temporary setting manually configure exposure
	 */
	public double exposureFeed;

	public double fastMovementFeed;

	public int bedSizeX;
	public int bedSizeY;

	public String laserOn;
	public String laserOff;

	public double cameraOffsetX;
	public double cameraOffsetY;
}
