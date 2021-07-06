package com.github.ruediste.laserPcb.gCode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class GCodeWriterTest {

	@Test
	public void testSplitGCodeText() throws Exception {
		var gCode = new GCodeWriter();
		assertEquals(List.of("M200"), gCode.splitGCodeText("M200"));
		assertEquals(List.of("M200", "M201"), gCode.splitGCodeText("M200\r\nM201"));
		assertEquals(List.of("M200", "M201"), gCode.splitGCodeText("M200\nM201"));

		assertEquals(List.of("M200", "M201"), gCode.splitGCodeText("M200 ; comment test\nM201;second line"));

	}
}
