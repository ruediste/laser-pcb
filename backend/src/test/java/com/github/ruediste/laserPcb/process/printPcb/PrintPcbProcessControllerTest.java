package com.github.ruediste.laserPcb.process.printPcb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.util.AffineTransformation;

import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PcbLayer;

public class PrintPcbProcessControllerTest {

	PrintPcbProcessController ctrl;

	@BeforeEach
	public void before() {
		ctrl = new PrintPcbProcessController();
	}

	@Test
	public void calculateTransformation() {
		List<CoordinatePoint> points = List.of(CoordinatePoint.of(4, 4), CoordinatePoint.of(5, 4),
				CoordinatePoint.of(3, 6), CoordinatePoint.of(3, 7));
		Rectangle2D bounds = new Rectangle2D.Double(10, 15, 100, 200);

		AffineTransformation t = ctrl.service.calculateTransformation(PcbLayer.TOP, points, bounds);

		check(t, 10, 215, 3, 4);
		check(t, 20, 215, 13, 4);
		check(t, 10, 210, 3, 9);

	}

	private void check(AffineTransformation t, double x, double y, double eX, double eY) {
		Coordinate src = new Coordinate(x, y);
		Coordinate dest = new Coordinate();
		t.transform(src, dest);
		assertEquals(eX, dest.x, 0.001, "X");
		assertEquals(eY, dest.y, 0.001, "Y");
	}
}
