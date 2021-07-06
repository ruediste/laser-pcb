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

		// get the line intersection
		CoordinatePoint origin = CoordinatePoint.lineIntersection(points.get(0), points.get(1), points.get(2),
				points.get(3));
		if (Double.isNaN(origin.x) || Double.isNaN(origin.y)) {
			log.error("Unable to calculate transformation for points {}", points);
			return null;
		}

		// calculate the base vector based on the point on the base line further away
		// from the origin
		double d0 = origin.vectorTo(points.get(0)).length2();
		double d1 = origin.vectorTo(points.get(1)).length2();

		CoordinateVector vBase;
		if (d0 < d1) {
			vBase = points.get(0).vectorTo(points.get(1)).normalize();
		} else
			vBase = points.get(1).vectorTo(points.get(0)).normalize();

		if (layer == PcbLayer.TOP) {

			// origin is in bottom left corner
			return new AffineTransformationBuilder(

					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY()), // origin
					new Coordinate(imageBounds.getMinX() + 1, imageBounds.getMinY()), // on base
					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY() + 1), // on side

					toCoordinate(origin), // origin
					toCoordinate(origin.plus(vBase)), // on base
					toCoordinate(origin.plus(vBase.normal())) // on side
			).getTransformation();
		} else {
			// origin is in bottom right corner
			return new AffineTransformationBuilder(

//					new Coordinate(imageBounds.getMaxX(), imageBounds.getMaxY()), // origin
//					new Coordinate(imageBounds.getMaxX() - 1, imageBounds.getMaxY()), // on base
//					new Coordinate(imageBounds.getMinX(), imageBounds.getMaxY() + 1), // on side

					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY()), // origin
					new Coordinate(imageBounds.getMinX() + 1, imageBounds.getMinY()), // on base
					new Coordinate(imageBounds.getMinX(), imageBounds.getMinY() + 1), // on side

					toCoordinate(origin), // origin
					toCoordinate(origin.plus(vBase)), // on base
					toCoordinate(origin.plus(vBase.normal().negate())) // on side
			).getTransformation();
		}
	}

	private Coordinate toCoordinate(CoordinatePoint origin) {
		return new Coordinate(origin.x, origin.y);
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
