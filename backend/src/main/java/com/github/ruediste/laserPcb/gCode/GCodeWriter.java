package com.github.ruediste.laserPcb.gCode;

import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
