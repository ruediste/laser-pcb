package com.github.ruediste.laserPcb.process.printPcb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.util.AffineTransformation;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;

public class PrintPcbProcessServiceTest {
	/*
	 * Both the PCB and GRBL coordinate system have X+ to the right and Y+ to the
	 * top
	 */

	PrintPcbProcessService service;

	/** origin is at 3,4 */
	List<CoordinatePoint> cornerPoints = List.of(CoordinatePoint.of(4, 4), CoordinatePoint.of(5, 4),
			CoordinatePoint.of(3, 6), CoordinatePoint.of(3, 7));

	/**
	 * BL: (10,15) BR: (110,15)
	 */
	Rectangle2D bounds = new Rectangle2D.Double(10, 15, 100, 200);

	@BeforeEach
	public void before() {
		service = new PrintPcbProcessService();
	}

	@Test
	public void calculateTransformation_bl_bl() {
		// origin is at 3,4

		AffineTransformation t = service.calculateTransformation(Corner.BL, Corner.BL, cornerPoints, bounds, 0);

		check(t, 10, 15, 3, 4);
		check(t, 20, 15, 13, 4);
		check(t, 10, 20, 3, 9);

		// check with bounds
		t = service.calculateTransformation(Corner.BL, Corner.BL, cornerPoints, bounds, 5);

		check(t, 10, 15, 3 + 5, 9);
		check(t, 20, 15, 13 + 5, 9);
		check(t, 10, 20, 3 + 5, 14);
	}

	@Test
	public void calculateTransformation_br_br() {
		// origin is at 3,4

		AffineTransformation t = service.calculateTransformation(Corner.BR, Corner.BR, cornerPoints, bounds, 0);

		check(t, 10, 15, 3 - 100, 4);
		check(t, 20, 15, 13 - 100, 4);
		check(t, 10, 20, 3 - 100, 9);

		// check with bounds
		t = service.calculateTransformation(Corner.BR, Corner.BR, cornerPoints, bounds, 5);

		check(t, 10, 15, 3 - 5 - 100, 9);
		check(t, 20, 15, 13 - 5 - 100, 9);
		check(t, 10, 20, 3 - 5 - 100, 14);
	}

	@Test
	public void calculateTransformation_bl_br() {
		// origin is at 3,4

		AffineTransformation t = service.calculateTransformation(Corner.BL, Corner.BR, cornerPoints, bounds, 0);

		check(t, 10, 15, 3 + 100, 4);
		check(t, 20, 15, 3 - 10 + 100, 4);
		check(t, 10, 20, 3 + 100, 9);

		// check with bounds
		t = service.calculateTransformation(Corner.BL, Corner.BR, cornerPoints, bounds, 5);

		check(t, 10, 15, 3 + 5 + 100, 9);
		check(t, 20, 15, 3 - 10 + 5 + 100, 9);
		check(t, 10, 20, 3 + 5 + 100, 14);
	}

	@Test
	public void calculateTransformation_br_bl() {
		// origin is at 3,4

		AffineTransformation t = service.calculateTransformation(Corner.BR, Corner.BL, cornerPoints, bounds, 0);

		check(t, 10, 15, 3, 4);
		check(t, 20, 15, 3 - 10, 4);
		check(t, 10, 20, 3, 9);

		// check with bounds
		t = service.calculateTransformation(Corner.BR, Corner.BL, cornerPoints, bounds, 5);

		check(t, 10, 15, 3 - 5, 9);
		check(t, 20, 15, 3 - 10 - 5, 9);
		check(t, 10, 20, 3 - 5, 14);
	}

	private void check(AffineTransformation t, double x, double y, double eX, double eY) {
		Coordinate src = new Coordinate(x, y);
		Coordinate dest = new Coordinate();
		t.transform(src, dest);
		assertEquals(eX, dest.x, 0.001, "X");
		assertEquals(eY, dest.y, 0.001, "Y");
	}
}
