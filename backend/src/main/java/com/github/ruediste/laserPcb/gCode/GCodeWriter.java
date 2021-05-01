package com.github.ruediste.laserPcb.gCode;

import java.util.ArrayList;
import java.util.List;

public class GCodeWriter {
	private List<String> gCodes = new ArrayList<>();
	Double lastFeedG0;
	Double lastFeedG1;
	Double lastFeed;

	public List<String> getGCodes() {
		return gCodes;
	}

	private String formatFeed(double value) {
		return "" + (int) Math.round(value);
	}

	private String formatCoordinate(double value) {
		return String.format("%.2f", value);
	}

	/**
	 * Rapid Motion
	 */
	public void g0(double f) {
		g0(null, null, null, f);
	}

	/**
	 * Rapid Motion
	 */
	public GCodeWriter g0(Double x, Double y) {
		return g0(x, y, null, null);
	}

	/**
	 * Rapid Motion
	 */
	public GCodeWriter g0(Double x, Double y, Double z, Double f) {
		String gCode = "G0";
		if (x != null)
			gCode += " X" + formatCoordinate(x);
		if (y != null)
			gCode += " Y" + formatCoordinate(y);
		if (z != null)
			gCode += " Z" + formatCoordinate(z);

		if (f == null)
			f = lastFeedG0;
		else
			lastFeedG0 = f;

		if (f != null && !f.equals(lastFeed)) {
			gCode += " F" + formatFeed(f);
			lastFeed = f;
		}
		gCodes.add(gCode);
		return this;
	}

	/**
	 * Coordinated Motion
	 */
	public void g1(double f) {
		g1(null, null, null, f);
	}

	/**
	 * Coordinated Motion
	 */
	public GCodeWriter g1(Double x, Double y) {
		return g1(x, y, null, null);
	}

	/**
	 * Coordinated Motion
	 */
	public GCodeWriter g1(Double x, Double y, Double z, Double f) {
		String gCode = "G1";
		if (x != null)
			gCode += " X" + formatCoordinate(x);
		if (y != null)
			gCode += " Y" + formatCoordinate(y);
		if (z != null)
			gCode += " Z" + formatCoordinate(z);

		if (f == null)
			f = lastFeedG1;
		else
			lastFeedG1 = f;

		if (f != null && !f.equals(lastFeed)) {
			gCode += " F" + formatFeed(f);
			lastFeed = f;
		}
		gCodes.add(gCode);
		return this;
	}

	public GCodeWriter add(String gCode) {
		gCodes.add(gCode);
		return this;
	}
}
