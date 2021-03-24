package com.github.ruediste.laserPcb.cnc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

public class CncConnectionTest {

	@Test
	public void test() {
		Matcher matcher = CncConnection.statusLinePatternGrbl
				.matcher("<Idle|MPos:0.000,0.000,0.000|FS:0,0|WCO:50.800,0.000,0.000>");
		assertTrue(matcher.matches());
	}
}
