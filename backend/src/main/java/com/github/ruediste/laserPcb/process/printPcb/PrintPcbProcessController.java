package com.github.ruediste.laserPcb.process.printPcb;

import static java.util.stream.Collectors.toList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jfree.svg.MeetOrSlice;
import org.jfree.svg.PreserveAspectRatio;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.ViewBox;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.overlay.OverlayOp;
import org.locationtech.jts.operation.overlayng.OverlayNGRobust;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.jts.JtsAdapter;
import com.github.ruediste.gerberLib.jts.MoveGenerator;
import com.github.ruediste.gerberLib.jts.MoveHandler;
import com.github.ruediste.gerberLib.parser.GerberParser;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter.Attribute;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;
import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
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
	ProcessController processAppCtrl;

	private ExecutorService executor = Executors.newCachedThreadPool();

	@PostConstruct
	void init() {
		ArrayList<CompletableFuture<?>> tasks = new ArrayList<>();
		processAppCtrl.update(process -> {
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
			var process = processAppCtrl.get().printPcb;
			if (process != null && current != null && process.canStartProcessing(current))
				startProcessFiles();
		});
	}

	@PreDestroy
	void preDestroy() {
		executor.shutdownNow();
	}

	public PrintPcbProcess get() {
		var process = processAppCtrl.get();
		if (process.printPcb == null)
			throw new RuntimeException("PrintPCB process not active");

		return process.printPcb;
	}

	public void update(Consumer<PrintPcbProcess> updater) {
		processAppCtrl.update(process -> {
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

	public static class InputImageData {
		public Geometry image;
		public String inputSvg;

		public Rectangle2D imageBounds;

		public ArrayList<Geometry> buffers;
		public String buffersSvg;
		public String buffersSvgHash;
		public String imageSvg;
		public String imageSvgHash;
		public String inputSvgHash;
	}

	ConcurrentHashMap<UUID, InputImageData> imageData = new ConcurrentHashMap<>();

	private void parse(PrintPcbInputFile file) {
		try {
			String input = Files.readString(fileUploadService.getPath(file.id));
			WarningCollector warningCollector = new WarningCollector();
			JtsAdapter jtsAdapter = new JtsAdapter();
			GerberReadGraphicsAdapter readAdapter = new GerberReadGraphicsAdapter(warningCollector,
					new GerberReadGeometricPrimitiveAdapter(warningCollector, jtsAdapter));
			new GerberParser(readAdapter, input).file();

			if (!warningCollector.warnings.isEmpty()) {
				log.info("Warnings while processing {}:\n{}", file.name, warningCollector);
			}
			ShapeWriter writer = new ShapeWriter();
			InputImageData data = new InputImageData();
			data.image = jtsAdapter.image();
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
					}
				}
				imageData.put(file.id, data);
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
		imageData.remove(id);
		update(process -> process.inputFiles.removeIf(file -> file.id.equals(id)));
	}

	public InputImageData getImageData(UUID id) {
		return imageData.get(id);
	}

	public void startProcessFiles() {
		Profile currentProfile = profileRepo.getCurrent();
		if (currentProfile == null)
			throw new RuntimeException("No active profile");
		List<Runnable> tasks = new ArrayList<>();
		update(process -> {
			if (!process.canStartProcessing(currentProfile))
				throw new RuntimeException("Files not ready to be processed");
			process.status = PrintPcbStatus.PROCESSING_FILES;
			combinedBounds = null;
			for (var file : process.inputFiles) {
				if (!file.status.canStartProcessing)
					continue;
				InputImageData data = imageData.get(file.id);
				if (data == null)
					continue;
				if (data.image == null)
					continue;
				data.imageSvg = null;
				data.buffersSvg = null;

				file.status = InputFileStatus.PROCESSING;
				tasks.add(() -> processFile(file, data, currentProfile));
				if (combinedBounds == null)
					combinedBounds = data.imageBounds;
				else
					combinedBounds = combinedBounds.createUnion(data.imageBounds);
			}
		});

		combinedSvgTargetWidth = 1000;
		combinedScale = combinedSvgTargetWidth / combinedBounds.getWidth();
		combinedSvgTargetHeight = (int) (combinedBounds.getHeight() * combinedScale);

		CompletableFuture<Void> allFiles = CompletableFuture
				.allOf(tasks.stream().map(r -> CompletableFuture.runAsync(r, executor)).collect(toList())
						.toArray(new CompletableFuture<?>[] {}));
		allFiles.whenCompleteAsync((r, t) -> {
			update(p -> {
				if (t == null)
					p.status = PrintPcbStatus.EXPOSING;
				else {
					log.error("Error while processing", t);
					p.status = PrintPcbStatus.INITIAL;
				}
			});

		}, executor);
	}

	Rectangle2D combinedBounds;

	private int combinedSvgTargetWidth;

	private double combinedScale;

	private int combinedSvgTargetHeight;

	private void processFile(PrintPcbInputFile file, InputImageData data, Profile currentProfile) {
		try {

			double toolDiameter = currentProfile.exposureWidth;
			double overlap = currentProfile.exposureOverlap;

			// calculate buffers
			var buffers = new ArrayList<Geometry>();
			Geometry image = data.image;
			Geometry remaining;
			{

				// do first buffer
				Geometry buffer = image.buffer(-toolDiameter / 2);
				if (!buffer.isEmpty())
					buffers.add(buffer);

				// remove the area covered by first buffer, to avoid having outermost remaining
				// areas filled
				remaining = image.buffer(-toolDiameter);

				// remaining buffers
				while (true) {
					buffer = buffer.buffer(-(toolDiameter * (1 - overlap)));
					if (buffer.isEmpty())
						break;
					buffers.add(buffer);
					remaining = OverlayNGRobust.overlay(remaining, buffer.getBoundary().buffer(toolDiameter / 2),
							OverlayOp.DIFFERENCE);
				}

				// fill the remaining areas
				while (!remaining.isEmpty()) {
					log.info("cls: {}", remaining.getClass());
					Geometry newRemaining = remaining;
					var nonCovered = new ArrayList<Geometry>();
					for (int n = 0; n < remaining.getNumGeometries(); n++) {
						Geometry g = remaining.getGeometryN(n);
						Envelope envelope = g.getEnvelopeInternal();
						if (envelope.getDiameter() < toolDiameter) {
							buffers.add(image.getFactory().createPoint(envelope.centre()));
							// TODO: replace by circle
							newRemaining = OverlayNGRobust.overlay(newRemaining, g.getEnvelope(), OverlayOp.DIFFERENCE);
							continue;
						}
						if (g instanceof Polygon) {
							Polygon p = (Polygon) g;
							buffers.add(p);
							newRemaining = OverlayNGRobust.overlay(newRemaining,
									p.getBoundary().buffer(toolDiameter / 2), OverlayOp.DIFFERENCE);
						}
						nonCovered.add(g);
					}
//					remaining = new GeometryCollection(nonCovered.toArray(new Geometry[] {}), image.getFactory());
					remaining = newRemaining;
				}
			}

			data.buffers = buffers;
			ShapeWriter writer = new ShapeWriter();

			// create svg of buffers
			{
				float lineWidth = (float) toolDiameter;
				SVGGraphics2D svg = createEmptyCombinedSvg();
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

				svg.setColor(Color.GREEN);
				svg.fill(writer.toShape(remaining));

				data.buffersSvg = toCombinedSvgString(svg);
				data.buffersSvgHash = sha256(data.buffersSvg);
			}

			// create svg of layer
			{
				SVGGraphics2D svg = createEmptyCombinedSvg();
				svg.setColor(Color.black);
				svg.fill(writer.toShape(data.image));
				data.imageSvg = toCombinedSvgString(svg);
				data.imageSvgHash = sha256(data.buffersSvg);
			}

			update(p -> file.status = InputFileStatus.PROCESSED);
		} catch (Throwable t) {
			log.error("Error wile processing {}", file.name, t);
			update(p -> {
				file.status = InputFileStatus.ERROR_PROCESSING;
				file.errorMessage = t.getMessage();
			});
			throw t;
		}
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

	public static String sha256(String base) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(base.getBytes("UTF-8"));
			return Base64.getUrlEncoder().encodeToString(hash);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private String toCombinedSvgString(SVGGraphics2D svg) {
		return svg.getSVGElement(null, false, new ViewBox(0, 0, combinedSvgTargetWidth, combinedSvgTargetHeight),
				PreserveAspectRatio.XMID_YMID, MeetOrSlice.MEET);
	}

	private SVGGraphics2D createEmptyCombinedSvg() {
		SVGGraphics2D svg = new SVGGraphics2D(combinedSvgTargetWidth, combinedSvgTargetHeight);
		svg.translate(0, combinedSvgTargetHeight);
		svg.scale(combinedScale, -combinedScale);
		svg.translate(-combinedBounds.getMinX(), -combinedBounds.getMinY());
		return svg;
	}

	private void generateGCode(InputImageData data, ArrayList<Geometry> buffers) throws IOException {
		WarningCollector warningCollector = new WarningCollector();
		var ncStream = new ByteArrayOutputStream();
		try (var out = new OutputStreamWriter(ncStream, StandardCharsets.UTF_8)) {

			out.append("G0 F1000\nG1 F100\n");
			MoveGenerator moveGenerator = new MoveGenerator(warningCollector, new MoveHandler() {

				@Override
				public void moveTo(Coordinate coordinate) {
					try {
						out.append(String.format("G0 X%.2f Y%.2f\n", coordinate.x, coordinate.y));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				@Override
				public void lineTo(Coordinate coordinate) {
					try {
						out.append(String.format("G1 X%.2f Y%.2f\n", coordinate.x, coordinate.y));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});

			buffers.forEach(moveGenerator::add);
			moveGenerator.generateMoves(new Coordinate(0, 0));
		}
		byte[] gCode = ncStream.toByteArray();
	}

}
