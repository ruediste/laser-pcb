package com.github.ruediste.laserPcb.process.printPcb;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.geom.util.AffineTransformationBuilder;
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
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PcbLayer;
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

	public AffineTransformation calculateTransformation(PcbLayer layer, List<CoordinatePoint> points,
			Rectangle2D imageBounds) {
		AffineTransformation transformation;
		CoordinatePoint origin = CoordinatePoint.lineIntersection(points.get(0), points.get(1), points.get(2),
				points.get(3));
		if (Double.isNaN(origin.x) || Double.isNaN(origin.y)) {
			log.error("Unable to calculate transformation for points {}", points);
			return null;
		}
		double d0 = origin.vectorTo(points.get(0)).length2();
		double d1 = origin.vectorTo(points.get(1)).length2();

		CoordinateVector vBase;
		if (d0 < d1) {
			vBase = points.get(0).vectorTo(points.get(1)).normalize();
		} else
			vBase = points.get(1).vectorTo(points.get(0)).normalize();

		if (layer == PcbLayer.TOP)
			transformation = new AffineTransformationBuilder(
					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY()),
					new Coordinate(imageBounds.getMinX() + 1, imageBounds.getMinY()),
					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY() + 1), toCoordinate(origin),
					toCoordinate(origin.plus(vBase)), toCoordinate(origin.plus(vBase.normal()))).getTransformation();
		else
			transformation = new AffineTransformationBuilder(
					new Coordinate(imageBounds.getMaxX(), imageBounds.getMaxY()),
					new Coordinate(imageBounds.getMaxX() - 1, imageBounds.getMaxY()),
					new Coordinate(imageBounds.getMinX(), imageBounds.getMaxY() - 1), toCoordinate(origin),
					toCoordinate(origin.plus(vBase)), toCoordinate(origin.plus(vBase.normal().negate())))
							.getTransformation();
		return transformation;
	}

	private Coordinate toCoordinate(CoordinatePoint origin) {
		return new Coordinate(origin.x, origin.y);
	}

	public GCodeWriter generateGCode(InputFileData data, AffineTransformation transformation) {
		Profile profile = profileRepo.getCurrent();
		WarningCollector warningCollector = new WarningCollector();
		var gCodes = new GCodeWriter();
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
					gCodes.add(profile.laserOff);
					laserOn = false;
				}
				gCodes.g0(transformed.x, transformed.y);
			}

			@Override
			public void lineTo(Coordinate raw) {
				Coordinate transformed = new Coordinate();
				transformation.transform(raw, transformed);
				if (!laserOn) {
					gCodes.add(profile.laserOn);
					laserOn = true;
				}
				gCodes.g1(transformed.x, transformed.y);
			}
		});

		data.buffers.forEach(moveGenerator::add);
		moveGenerator.generateMoves(new Coordinate(0, 0));
		return gCodes;
	}

}
