package com.github.ruediste.laserPcb.process.printPcb;

import java.awt.geom.Rectangle2D;

public class Rectangle {

	public double x, y, w, h;

	public Rectangle() {
	}

	public Rectangle(Rectangle2D r) {
		this.x = r.getX();
		this.y = r.getY();
		this.w = r.getWidth();
		this.h = r.getHeight();
	}

	public Rectangle2D toRectangle2d() {
		return new Rectangle2D.Double(x, y, w, h);
	}
}
