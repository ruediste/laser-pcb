package com.github.ruediste.laserPcb.process.printPcb;

import static java.util.stream.Collectors.toList;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jfree.svg.MeetOrSlice;
import org.jfree.svg.PreserveAspectRatio;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.jts.JtsAdapter;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;
import com.github.ruediste.gerberLib.linAlg.CoordinateTransformation;
import com.github.ruediste.gerberLib.parser.GerberParser;
import com.github.ruediste.gerberLib.parser.InputPosition;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter.Attribute;
import com.github.ruediste.gerberLib.readGeometricPrimitive.CompoundGerberReadGeometricPrimitiveEventHandler;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;
import com.github.ruediste.laserPcb.Var;
import com.github.ruediste.laserPcb.cnc.CncConnection;
import com.github.ruediste.laserPcb.cnc.CncConnection.CncState;
import com.github.ruediste.laserPcb.cnc.CncConnectionAppController;
import com.github.ruediste.laserPcb.cnc.SendGCodeController;
import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
import com.github.ruediste.laserPcb.gCode.GCodeWriter;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.process.ProcessRepository;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.InputFileStatus;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PcbLayer;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbStatus;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@Service
public class PrintPcbProcessController {
	private final Logger log = LoggerFactory.getLogger(PrintPcbProcessController.class);

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

	@Autowired
	PrintPcbProcessService service;

	private ExecutorService executor = Executors.newCachedThreadPool();

	@PostConstruct
	void init() {
		ArrayList<CompletableFuture<?>> tasks = new ArrayList<>();
		processCtrl.update(process -> {
			if (process.printPcb != null) {
				process.printPcb.status = PrintPcbStatus.INITIAL;
				for (var file : process.printPcb.inputFiles) {
					file.status = InputFileStatus.PARSING;
					tasks.add(CompletableFuture.runAsync(() -> parse(file), executor));
				}
			}
		});

		CompletableFuture.allOf(tasks.toArray(new CompletableFuture[] {})).thenRunAsync(() -> {
			Profile current = profileRepo.getCurrent();
			var process = processCtrl.get().printPcb;
			if (process != null && current != null && process.canStartProcessing(current))
				startProcessFiles();
		});
	}

	@PreDestroy
	void preDestroy() {
		executor.shutdownNow();
	}

	public PrintPcbProcess get() {
		var process = processCtrl.get();
		if (process.printPcb == null)
			throw new RuntimeException("PrintPCB process not active");

		return process.printPcb;
	}

	public void update(Consumer<PrintPcbProcess> updater) {
		processCtrl.update(process -> {
			if (process.printPcb == null)
				throw new RuntimeException("PrintPCB process not active");
			updater.accept(process.printPcb);
		});
	}

	public void addFile(PrintPcbInputFile file) {
		file.status = InputFileStatus.PARSING;
		update(process -> process.inputFiles.add(file));
		executor.submit(() -> parse(file));
	}

	/**
	 * In-Memory data for an input file.
	 */
	public static class InputFileData {
		public Geometry image;
		public String inputSvg;

		public Rectangle2D imageBounds;

		public ArrayList<Geometry> buffers;
		public String buffersSvg;
		public String buffersSvgHash;
		public String imageSvg;
		public String imageSvgHash;
		public String inputSvgHash;
		public Geometry imageDrill;
	}

	ConcurrentHashMap<UUID, InputFileData> fileData = new ConcurrentHashMap<>();

	private void parse(PrintPcbInputFile file) {
		try {
			Profile profile = profileRepo.getCurrent();
			String input = Files.readString(fileUploadService.getPath(file.id));
			WarningCollector warningCollector = new WarningCollector();
			JtsAdapter jtsAdapter = new JtsAdapter();
			JtsAdapter jtsAdapterDrill = new JtsAdapter() {

				private double toDrillSize(double size) {
					double result = size * profile.drillScale - profile.drillOffset;
					if (result < profile.minDrillSize)
						return profile.minDrillSize;
					if (result > profile.maxDrillSize)
						return profile.maxDrillSize;
					return result;
				}

				@Override
				public void addArc(InputPosition pos, CoordinateTransformation transformation, CoordinatePoint p,
						double w, double h, double angSt, double angExt) {
					CoordinatePoint center = p.plus(w / 2, h / 2);
					w = toDrillSize(w);
					h = toDrillSize(h);
					super.addArc(pos, transformation, center.minus(w / 2, h / 2), w, h, angSt, angExt);
				}
			};

			GerberReadGraphicsAdapter readAdapter = new GerberReadGraphicsAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector,
							new CompoundGerberReadGeometricPrimitiveEventHandler(jtsAdapter, jtsAdapterDrill)));
			new GerberParser(readAdapter, input).file();

			if (!warningCollector.warnings.isEmpty()) {
				log.info("Warnings while processing {}:\n{}", file.name, warningCollector);
			}
			ShapeWriter writer = new ShapeWriter();
			InputFileData data = new InputFileData();
			data.image = jtsAdapter.image();
			data.imageDrill = jtsAdapterDrill.image();
			Shape imageShape = writer.toShape(data.image);
			Rectangle2D bounds = imageShape.getBounds2D();
			data.imageBounds = bounds;

			int targetWidth = 1000;
			double scale = targetWidth / bounds.getWidth();
			int targetHeight = (int) (bounds.getHeight() * scale);

			SVGGraphics2D svg = new SVGGraphics2D(targetWidth, targetHeight);
			svg.translate(0, targetHeight);
			svg.scale(scale, -scale);
			svg.translate(-bounds.getMinX(), -bounds.getMinY());

			svg.setColor(Color.black);
			svg.fill(imageShape);
			data.inputSvg = svg.getSVGElement(null, false, new ViewBox(0, 0, targetWidth, targetHeight),
					PreserveAspectRatio.XMID_YMID, MeetOrSlice.MEET);
			data.inputSvgHash = sha256(data.inputSvg);

			update(process -> {
				if (!process.inputFiles.stream().anyMatch(f -> f.id.equals(file.id))) {
					// image has been removed in the mean time
					return;
				}
				Attribute attr = readAdapter.attributes.get(".FileFunction");
				if (attr != null && attr.values.size() >= 3) {
					if ("Copper".equalsIgnoreCase(attr.values.get(0))) {
						if (attr.values.stream().anyMatch(x -> "Top".equalsIgnoreCase(x)))
							file.layer = PcbLayer.TOP;
						else if (attr.values.stream().anyMatch(x -> "Bot".equalsIgnoreCase(x)))
							file.layer = PcbLayer.BOTTOM;
					} else if (attr.values.stream().anyMatch(x -> "Drill".equalsIgnoreCase(x)))
						file.layer = PcbLayer.DRILL;
				}
				fileData.put(file.id, data);
				file.status = InputFileStatus.PARSED;
				file.errorMessage = null;
			});
		} catch (Throwable t) {
			log.error("Error wile parsing {}", file.name, t);
			update(process -> {
				file.status = InputFileStatus.ERROR_PARSING;
				file.errorMessage = t.getMessage();
			});
		}
	}

	public void removeFile(UUID id) {
		fileData.remove(id);
		update(process -> process.inputFiles.removeIf(file -> file.id.equals(id)));
	}

	public InputFileData getImageData(UUID id) {
		return fileData.get(id);
	}

	public void startProcessFiles() {
		Profile currentProfile = profileRepo.getCurrent();
		if (currentProfile == null)
			throw new RuntimeException("No active profile");
		List<Runnable> tasks = new ArrayList<>();

		// calculate combined size
		CombinedImageSize combinedSize = new CombinedImageSize();

		for (var file : get().inputFiles) {
			InputFileData data = fileData.get(file.id);
			if (data == null)
				continue;
			if (combinedSize.combinedBounds == null)
				combinedSize.combinedBounds = data.imageBounds;
			else
				combinedSize.combinedBounds = combinedSize.combinedBounds.createUnion(data.imageBounds);
		}
		combinedSize.combinedSvgTargetWidth = 1000;
		combinedSize.combinedScale = combinedSize.combinedSvgTargetWidth / combinedSize.combinedBounds.getWidth();
		combinedSize.combinedSvgTargetHeight = (int) (combinedSize.combinedBounds.getHeight()
				* combinedSize.combinedScale);

		// create processing tasks
		update(process -> {
			if (!process.canStartProcessing(currentProfile))
				throw new RuntimeException("Files not ready to be processed");
			process.status = PrintPcbStatus.PROCESSING_FILES;
			PrintPcbInputFile drillFile = process.fileMap().get(PcbLayer.DRILL);
			InputFileData drillFileData;
			if (drillFile != null)
				drillFileData = fileData.get(drillFile.id);
			else
				drillFileData = null;

			for (var file : process.inputFiles) {
				if (!file.status.canStartProcessing)
					continue;
				if (!file.layer.isCopperLayer)
					continue;
				InputFileData data = fileData.get(file.id);
				if (data == null)
					continue;
				if (data.image == null)
					continue;
				data.imageSvg = null;
				data.buffersSvg = null;

				file.status = InputFileStatus.PROCESSING;
				tasks.add(() -> {
					try {
						service.processFile(file, data, currentProfile, drillFileData, combinedSize);
						update(p -> file.status = InputFileStatus.PROCESSED);
					} catch (Throwable t) {
						log.error("Error wile processing {}", file.name, t);
						update(p -> {
							file.status = InputFileStatus.ERROR_PROCESSING;
							file.errorMessage = t.getMessage();
						});
						throw t;
					}
				});

			}
		});

		// submit tasks and wait for completion
		CompletableFuture<Void> allFiles = CompletableFuture
				.allOf(tasks.stream().map(r -> CompletableFuture.runAsync(r, executor)).collect(toList())
						.toArray(new CompletableFuture<?>[] {}));
		allFiles.whenCompleteAsync((r, t) -> {
			update(p -> {
				if (t == null)
					p.status = PrintPcbStatus.FILES_PROCESSED;
				else {
					log.error("Error while processing", t);
					p.status = PrintPcbStatus.INITIAL;
				}
			});

		}, executor);
	}

	public static String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			return Base64.getUrlEncoder().encodeToString(hash);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void startExposing() {
		Profile profile = profileRepo.getCurrent();
		update(p -> {
			if (p.status != PrintPcbStatus.FILES_PROCESSED)
				throw new RuntimeException("Cannot start exposing in status " + p.status);

			p.status = PrintPcbStatus.POSITION_1;
			p.positionPoints.clear();

		});

		if (true) {
			goToCameraHeight();
		} else if (false) {
			// testing only
			update(p -> {
				p.positionPoints.addAll(List.of(CoordinatePoint.of(-1, 0), CoordinatePoint.of(-2, 0),
						CoordinatePoint.of(0, 1), CoordinatePoint.of(0, 2)));
				p.status = PrintPcbStatus.EXPOSING_2;
			});
			sendExposingCommands();
		} else {
			// testing only
			update(p -> {
				p.positionPoints.addAll(List.of(CoordinatePoint.of(1, 0), CoordinatePoint.of(2, 0),
						CoordinatePoint.of(0, 1), CoordinatePoint.of(0, 2)));
				p.status = PrintPcbStatus.EXPOSING_1;
			});
			sendExposingCommands();
		}
	}

	private void goToCameraHeight() {
		Profile profile = profileRepo.getCurrent();
		CncConnection conn = connCtrl.getConnection();
		GCodeWriter gCode = new GCodeWriter();
		gCode.absolutePositioning();
		gCode.unitsMM();
		gCode.g0(null, null, profile.cameraZ, profile.fastMovementFeed);
		conn.sendGCodes(gCode);
	}

	public void addPositionPoint() {
		CncState state = connCtrl.getConnection().getState();
		Profile profile = profileRepo.getCurrent();
		CoordinatePoint point = new CoordinatePoint(state.x - profile.cameraOffsetX, state.y - profile.cameraOffsetY);
		Var<Runnable> action = Var.of();
		update(p -> {
			if (p.status != PrintPcbStatus.POSITION_1 && p.status != PrintPcbStatus.POSITION_2)
				throw new RuntimeException("Cannot add position point in status " + p.status);
			if (p.positionPoints.size() >= 4)
				throw new RuntimeException("Cannot add more position points");
			p.positionPoints.add(point);
			if (p.positionPoints.size() >= 4) {
				switch (p.status) {
				case POSITION_1:
					p.status = PrintPcbStatus.EXPOSING_1;
					action.set(() -> sendExposingCommands());
					break;
				case POSITION_2:
					p.status = PrintPcbStatus.EXPOSING_2;
					action.set(() -> sendExposingCommands());
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
		});
		if (action.get() != null)
			action.get().run();
	}

	private void sendExposingCommands() {
		PrintPcbProcess process = get();
		Profile profile = profileRepo.getCurrent();
		Corner pcbCorner = process.currentPcbAlignmentCorner(profile);
		PcbLayer layer = process.currentLayer(profile);

		PrintPcbInputFile file = process.fileMap().get(layer);
		InputFileData data = this.fileData.get(file.id);

		// determine coordinate transformation

		AffineTransformation transformation = service.calculateTransformation(pcbCorner,
				layer == PcbLayer.TOP ? pcbCorner : pcbCorner.opposite(), process.positionPoints, data.imageBounds,
				profile.boardBorder);
		if (transformation == null) {
			update(p -> {
				p.status = PrintPcbStatus.FILES_PROCESSED;
				p.positionPoints.clear();
			});
			throw new RuntimeException("Error while calculating transformation");
		}
		log.info("Image Bounds: {}, Transformation: {}", data.imageBounds, transformation);

		// generate and send gcode
		GCodeWriter gCode = service.generateGCode(data, transformation);

		gCode.dumpToDebugFile();
		Var<Runnable> action = Var.of();
		sendGCodeController.sendGCodes(gCode).thenRun(() -> {
			update(p -> {
				p.positionPoints.clear();
				if (p.status == PrintPcbStatus.EXPOSING_2 || profileRepo.getCurrent().singleLayerPcb)
					p.status = PrintPcbStatus.FILES_PROCESSED;
				else {
					p.status = PrintPcbStatus.POSITION_2;
					action.set(this::goToCameraHeight);
				}
			});
		}).exceptionally(t -> {
			log.error("Error while sending exposing GCode", t);
			update(p -> {
				p.positionPoints.clear();
				p.status = PrintPcbStatus.FILES_PROCESSED;
			});
			return null;
		});
		if (action.get() != null)
			action.get().run();
	}

}
