package com.github.ruediste.laserPcb.profile;

import com.github.ruediste.laserPcb.EntityBase;
import com.github.ruediste.laserPcb.process.printPcb.Corner;

public class Profile extends EntityBase {

	public String name;
	public boolean singleLayerPcb;
	public Corner preferredAlignmentCorner = Corner.BL;

	public double laserZ;

	public int cameraRotation;

	public int baudRate;

	/**
	 * overlap between exposures (fraction of exposure width)
	 */
	public double exposureOverlap;

	/**
	 * With of a line drawn by the laser
	 */
	public double exposureWidth;

	/**
	 * Feed to use during exposure
	 */
	public double exposureFeed;

	public double fastMovementFeed;

	public int bedSizeX;
	public int bedSizeY;

	public String laserOn;
	public String laserOff;

	public String preExposeGCode;

	public double cameraOffsetX;
	public double cameraOffsetY;
	public double cameraZ;

	public double minDrillSize;
	public double maxDrillSize;
	public double drillOffset;
	public double drillScale = 1;

	public double boardBorder;
}
