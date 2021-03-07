package com.github.ruediste.laserPcb.motorcycle;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

public class MotorcycleGraphTest {

	public static class Vector {
		public final double x;
		public final double y;

		public Vector(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public Vector(Coordinate start, Coordinate end) {
			x = end.x - start.x;
			y = end.y - start.y;
		}

		public Vector rotate90() {
			return new Vector(-y, x);
		}

		public double length() {
			return Math.sqrt(x * x + y * y);
		}

		public Vector normalize() {
			double l = length();
			return new Vector(x / l, y / l);
		}

		public Vector plus(Vector other) {
			return new Vector(x + other.x, y + other.y);
		}
	}

	@Test
	public void test() {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
		CoordinateArraySequence ring = new CoordinateArraySequence(new Coordinate[] { new Coordinate(0, 0),
				new Coordinate(1, 0), new Coordinate(0, 1), new Coordinate(0, 0) });
		LinearRing shell = new LinearRing(ring, gf);
		// make shell counter clockwise
		if (!Orientation.isCCW(ring))
			shell = shell.reverse();

		// Polygon polygon = new Polygon(shell, new LinearRing[] {}, gf);

		Coordinate[] coordinates = shell.getCoordinates();
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate l = coordinates[(i - 1) % coordinates.length];
			Coordinate c = coordinates[i];
			Coordinate r = coordinates[(i + 1) % coordinates.length];
			Vector nl = new Vector(l, c).rotate90().normalize();
			Vector nr = new Vector(c, r).rotate90().normalize();
			Vector d = nl.plus(nr);

		}
	}
}
