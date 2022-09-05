package com.github.ruediste.laserPcb.gCode;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.laserPcb.profile.Profile;

public class GCodeWriter {
	private List<String> gCodes = new ArrayList<>();
	Double lastFeedG0;
	Double lastFeedG1;
	Double lastFeed;
	Integer laserIntensity;

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
	public GCodeWriter g0(Double x, Double y, Double z) {
		return g0(x, y, z, null);
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
		gCode += " S0";
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
	public GCodeWriter g1(Double x, Double y, Double z) {
		return g1(x, y, z, null);
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

		if (laserIntensity != null) {
			gCode += " S" + laserIntensity;
		}

		gCodes.add(gCode);
		return this;
	}

	public GCodeWriter add(String gCode) {
		gCodes.add(gCode);
		return this;
	}

	public void dumpToDebugFile() {
		String gCodeString = getGCodes().stream().collect(joining("\n"));

		// save to file for debugging purpose
		try {
			Files.writeString(new File("test.nc").toPath(), gCodeString, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public GCodeWriter absolutePositioning() {
		return add("G90");
	}

	public GCodeWriter relativePositioning() {
		return add("G91");
	}

	public GCodeWriter unitsMM() {
		return add("G21");
	}

	/**
	 * Set the fan speed
	 * 
	 * @param sValue value 0-255
	 */
	public GCodeWriter setFanSpeed(int sValue) {
		return add("M106 S" + sValue);
	}

	public GCodeWriter fanOff() {
		return add("M107");
	}

	public GCodeWriter laserOnJog(Profile profile) {
		return add("M3S" + profile.laserIntensity);
	}

	public GCodeWriter laserOn(Profile profile) {
		return laserOn(profile, profile.laserIntensity);
	}

	public GCodeWriter laserOn(Profile profile, int laserIntensity) {
		return laserOn(laserIntensity);
	}

	public GCodeWriter laserOn(int sValue) {
		laserIntensity = sValue;
		return add("M3IS0");
	}

	public GCodeWriter laserOff(Profile profile) {
		laserIntensity = null;
		return add("M5I");
	}

	public void splitAndAdd(String gCodeSource) {
		splitGCodeText(gCodeSource).forEach(this::add);
	}

	List<String> splitGCodeText(String gCodeSource) {
		List<String> gCodes = new ArrayList<>();
		for (String code : gCodeSource.split("\n")) {
			int idx = code.indexOf(';');
			if (idx >= 0)
				code = code.substring(0, idx);
			if (code.endsWith("\r"))
				code = code.substring(0, code.length() - 1);
			code = code.trim();
			if (code.isEmpty())
				continue;
			gCodes.add(code);
		}
		return gCodes;
	}
}
