package com.github.ruediste.laserPcb.process.printPcb;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jfree.svg.MeetOrSlice;
import org.jfree.svg.PreserveAspectRatio;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFilter;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.geom.util.AffineTransformationBuilder;
import org.locationtech.jts.operation.overlay.OverlayOp;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.jts.MoveGenerator;
import com.github.ruediste.gerberLib.jts.MoveHandler;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateVector;
import com.github.ruediste.laserPcb.cnc.CncConnectionAppController;
import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.process.ProcessRepository;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessController.InputFileData;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class PrintPcbProcessService {
	private final Logger log = LoggerFactory.getLogger(PrintPcbProcessService.class);

	@Autowired
	ProcessRepository repo;

	@Autowired
	FileUploadService fileUploadService;

	@Autowired
	ProfileRepository profileRepo;

	@Autowired
	ProcessController processCtrl;

	@Autowired
	CncConnectionAppController connCtrl;

	@Autowired
	SendGCodeController sendGCodeController;

	public AffineTransformation calculateTransformation(Corner pcbCorner, Corner imageCorner,
			List<CoordinatePoint> cornerPoints, Rectangle2D imageBounds, double boardBorder) {

		// get the line intersection
		CoordinatePoint origin = CoordinatePoint.lineIntersection(cornerPoints.get(0), cornerPoints.get(1),
				cornerPoints.get(2), cornerPoints.get(3));
		if (Double.isNaN(origin.x) || Double.isNaN(origin.y)) {
			log.error("Unable to calculate transformation for points {}", cornerPoints);
			return null;
		}

		// calculate the base vector based on the point on the base line further away
		// from the origin
		double d0 = origin.vectorTo(cornerPoints.get(0)).length2();
		double d1 = origin.vectorTo(cornerPoints.get(1)).length2();

		CoordinateVector vBase;
		if (d0 < d1) {
			vBase = origin.vectorTo(cornerPoints.get(1)).normalize();
		} else
			vBase = origin.vectorTo(cornerPoints.get(0)).normalize();

		// vBase always points towards the right
		if (vBase.x < 0)
			vBase = vBase.negate();

		// move origin to account for border
		switch (pcbCorner) {
		case BL:
			origin = origin.plus(vBase.scale(boardBorder)).plus(vBase.normal().scale(boardBorder));
			break;
		case BR:
			origin = origin.minus(vBase.scale(boardBorder)).plus(vBase.normal().scale(boardBorder));
			break;
		default:
			throw new UnsupportedOperationException();

		}

		// negate vBase if the corners are not equal, to create the mirroring effect
		CoordinateVector vBaseN = vBase.normal();
		if (pcbCorner != imageCorner)
			vBase = vBase.negate();

		Coordinate[] imageCoordinates = new Coordinate[3];
		Coordinate[] outpuCoordinates = new Coordinate[3];
		switch (imageCorner) {
		case BL:
			imageCoordinates[0] = new Coordinate(imageBounds.getMinX(), imageBounds.getMinY()); // origin
			imageCoordinates[1] = new Coordinate(imageBounds.getMinX() + 1, imageBounds.getMinY()); // on base
			imageCoordinates[2] = new Coordinate(imageBounds.getMinX(), imageBounds.getMinY() + 1); // on side
			break;
		case BR:
			imageCoordinates[0] = new Coordinate(imageBounds.getMaxX(), imageBounds.getMinY()); // origin
			imageCoordinates[1] = new Coordinate(imageBounds.getMaxX() + 1, imageBounds.getMinY()); // on base
			imageCoordinates[2] = new Coordinate(imageBounds.getMaxX(), imageBounds.getMinY() + 1); // on side
			break;
		default:
			throw new UnsupportedOperationException();
		}

		outpuCoordinates[0] = toCoordinate(origin); // origin
		outpuCoordinates[1] = toCoordinate(origin.plus(vBase)); // on base
		outpuCoordinates[2] = toCoordinate(origin.plus(vBaseN));// on side

		return new AffineTransformationBuilder(imageCoordinates[0], imageCoordinates[1], imageCoordinates[2],
				outpuCoordinates[0], outpuCoordinates[1], outpuCoordinates[2]).getTransformation();
	}

	private Coordinate toCoordinate(CoordinatePoint origin) {
		return new Coordinate(origin.x, origin.y);
	}

	public void processFile(PrintPcbInputFile file, InputFileData data, Profile currentProfile,
			InputFileData drillFileData, CombinedImageSize combinedSize) {

		double toolDiameter = currentProfile.exposureWidth;
		double overlap = currentProfile.exposureOverlap;

		// calculate buffers
		var buffers = new ArrayList<Geometry>();
		Geometry image = data.image;

		if (drillFileData != null)
			image = dropNon2D(OverlayNGRobust.overlay(image, drillFileData.imageDrill, OverlayOp.DIFFERENCE));
		Geometry remaining;
		{

			// do first buffer, place the first tool path half the tool diameter to the
			// inside
			Geometry buffer = image.buffer(-toolDiameter / 2);
			if (!buffer.isEmpty())
				buffers.add(simplyfyBuffer(toolDiameter, buffer));

			// remove the area covered by first buffer, to avoid having outermost remaining
			// areas filled
			// remaining = image.buffer(-toolDiameter);

			remaining = image;
			{
				Geometry areaCoveredByTool = buffer.getBoundary().buffer(1.01 * toolDiameter / 2); // factor for
																									// numerical
																									// stability
				remaining = dropNon2D(OverlayNGRobust.overlay(remaining, areaCoveredByTool, OverlayOp.DIFFERENCE));
			}

			// remaining buffers
			while (true) {
				buffer = buffer.buffer(-(toolDiameter * (1 - overlap)));

				if (buffer.isEmpty())
					break;
				buffers.add(simplyfyBuffer(toolDiameter, buffer));

				Geometry areaCoveredByTool = buffer.getBoundary().buffer(1.01 * toolDiameter / 2);
				try {
					remaining = dropNon2D(OverlayNGRobust.overlay(remaining, areaCoveredByTool, OverlayOp.DIFFERENCE));
				} catch (Exception e) {
					log.error("Error while updating remaining area", e);
					remaining.apply(new GeometryFilter() {

						@Override
						public void filter(Geometry geom) {
							log.info("Remaining  dim {}: {}", geom.getDimension(), geom);
						}
					});

					areaCoveredByTool.apply(new GeometryFilter() {

						@Override
						public void filter(Geometry geom) {
							log.info("Area covered by tool dim {}: {}", geom.getDimension(), geom);
						}
					});
				}
			}

			// fill the remaining areas
			while (!remaining.isEmpty()) {
				Geometry newRemaining = remaining;
				var nonCovered = new ArrayList<Geometry>();
				for (int n = 0; n < remaining.getNumGeometries(); n++) {
					Geometry g = remaining.getGeometryN(n);
					Envelope envelope = g.getEnvelopeInternal();
					if (envelope.getDiameter() < toolDiameter) {
						buffers.add(image.getFactory().createPoint(envelope.centre()));
						newRemaining = dropNon2D(
								OverlayNGRobust.overlay(newRemaining, g.getEnvelope(), OverlayOp.DIFFERENCE));
						continue;
					}
					if (g instanceof Polygon) {
						Polygon p = (Polygon) g;
						buffers.add(simplyfyBuffer(toolDiameter, p));
						newRemaining = dropNon2D(OverlayNGRobust.overlay(newRemaining,
								p.getBoundary().buffer(toolDiameter / 2), OverlayOp.DIFFERENCE));
					}
					nonCovered.add(g);
				}
				remaining = newRemaining;
			}
		}

		data.buffers = buffers;
		ShapeWriter writer = new ShapeWriter();

		// create svg of buffers
		{
			float lineWidth = (float) toolDiameter;
			SVGGraphics2D svg = createEmptyCombinedSvg(combinedSize);
//				svg.setColor(Color.BLACK);
//				svg.setStroke(new BasicStroke((float) (Math.abs(bufferDistance) * 2)));
//				buffers.forEach(b -> svg.draw(writer.toShape(b)));

			svg.setColor(Color.YELLOW);
			svg.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			buffers.stream().flatMap(notOfType(Point.class)).forEach(b -> svg.draw(writer.toShape(b)));

			svg.setColor(Color.RED);
			svg.setStroke(new BasicStroke(lineWidth / 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			buffers.stream().flatMap(notOfType(Point.class)).forEach(b -> svg.draw(writer.toShape(b)));

			buffers.stream().flatMap(ofType(Point.class))
					.forEach(point -> svg.fill(new Arc2D.Double(point.getX() - lineWidth / 2,
							point.getY() - lineWidth / 2, lineWidth, lineWidth, 0, 360, Arc2D.CHORD)));

			// debug output of the endpoints of line segments
			svg.setColor(Color.BLUE);
			for (var b : buffers) {
				b.apply(new CoordinateFilter() {

					@Override
					public void filter(Coordinate coord) {
						svg.fill(new Arc2D.Double(coord.x - lineWidth / 4, coord.y - lineWidth / 4, lineWidth / 2,
								lineWidth / 2, 0, 360, Arc2D.CHORD));
					}

				});
			}

			svg.setColor(Color.GREEN);
			svg.fill(writer.toShape(remaining));

			data.buffersSvg = toCombinedSvgString(svg, combinedSize);
			data.buffersSvgHash = PrintPcbProcessController.sha256(data.buffersSvg);
		}

		// create svg of layer
		{
			SVGGraphics2D svg = createEmptyCombinedSvg(combinedSize);
			svg.setColor(Color.black);
			svg.fill(writer.toShape(data.image));
			data.imageSvg = toCombinedSvgString(svg, combinedSize);
			data.imageSvgHash = PrintPcbProcessController.sha256(data.buffersSvg);
		}

	}

	private Geometry dropNon2D(Geometry input) {
		if (input instanceof GeometryCollection) {
			boolean filter = false;
			for (int i = 0; i < input.getNumGeometries(); i++) {
				if (input.getGeometryN(i).getDimension() != 2) {
					filter = true;
					break;
				}
			}
			if (filter) {
				ArrayList<Polygon> geometries = new ArrayList<>();
				for (int i = 0; i < input.getNumGeometries(); i++) {
					Geometry geometryN = input.getGeometryN(i);
					if (geometryN instanceof Polygon) {
						geometries.add((Polygon) geometryN);
					}
				}
				if (geometries.isEmpty())
					return new MultiPolygon(null, input.getFactory());
				else
					return new MultiPolygon(geometries.toArray(new Polygon[] {}), input.getFactory());
			}
		}
		return input;
	}

	private Geometry simplyfyBuffer(double toolDiameter, Geometry buffer) {
		return DouglasPeuckerSimplifier.simplify(buffer, toolDiameter / 5);
	}

	private String toCombinedSvgString(SVGGraphics2D svg, CombinedImageSize combinedSize) {
		return svg.getSVGElement(null, false,
				new ViewBox(0, 0, combinedSize.combinedSvgTargetWidth, combinedSize.combinedSvgTargetHeight),
				PreserveAspectRatio.XMID_YMID, MeetOrSlice.MEET);
	}

	private SVGGraphics2D createEmptyCombinedSvg(CombinedImageSize combinedSize) {
		SVGGraphics2D svg = new SVGGraphics2D(combinedSize.combinedSvgTargetWidth,
				combinedSize.combinedSvgTargetHeight);
		svg.translate(0, combinedSize.combinedSvgTargetHeight);
		svg.scale(combinedSize.combinedScale, -combinedSize.combinedScale);
		svg.translate(-combinedSize.combinedBounds.getMinX(), -combinedSize.combinedBounds.getMinY());
		return svg;
	}

	<T> Function<Object, Stream<T>> ofType(Class<T> cls) {
		return element -> {
			if (cls.isInstance(element)) {
				return Stream.of(cls.cast(element));
			}
			return Stream.empty();
		};
	}

	<T> Function<T, Stream<T>> notOfType(Class<? extends T> cls) {
		return element -> {
			if (!cls.isInstance(element)) {
				return Stream.of(element);
			}
			return Stream.empty();
		};
	}

	public GCodeWriter generateGCode(InputFileData data, AffineTransformation transformation) {
		Profile profile = profileRepo.getCurrent();
		WarningCollector warningCollector = new WarningCollector();
		var gCodes = new GCodeWriter();
		gCodes.splitAndAdd(profile.preExposeGCode);
		gCodes.add("G90"); // absolute positioning
		gCodes.add("G21"); // set units to millimeters
		gCodes.g0(profile.fastMovementFeed);
		gCodes.g1(profile.exposureFeed);

		gCodes.g0(null, null, profile.laserZ, null); // go to the right height

		MoveGenerator moveGenerator = new MoveGenerator(warningCollector, new MoveHandler() {

			boolean laserOn;

			@Override
			public void moveTo(Coordinate raw) {
				Coordinate transformed = new Coordinate();
				transformation.transform(raw, transformed);
				if (laserOn) {
					gCodes.laserOff(profile);
					laserOn = false;
				}
				gCodes.g0(transformed.x, transformed.y);
			}

			@Override
			public void lineTo(Coordinate raw) {
				Coordinate transformed = new Coordinate();
				transformation.transform(raw, transformed);
				if (!laserOn) {
					gCodes.laserOn(profile);
					laserOn = true;
				}
				gCodes.g1(transformed.x, transformed.y);
			}
		});

		data.buffers.forEach(moveGenerator::add);
		moveGenerator.generateMoves(new Coordinate(0, 0));
		gCodes.laserOff(profile);
		return gCodes;
	}

}
