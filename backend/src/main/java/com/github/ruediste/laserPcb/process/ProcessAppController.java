package com.github.ruediste.laserPcb.process;

import static java.util.stream.Collectors.toList;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.util.UUID;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.github.ruediste.gerberLib.WarningCollector;
import com.github.ruediste.gerberLib.jts.JtsAdapter;
import com.github.ruediste.gerberLib.parser.GerberParser;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter;
import com.github.ruediste.gerberLib.read.GerberReadGraphicsAdapter.Attribute;
import com.github.ruediste.gerberLib.readGeometricPrimitive.GerberReadGeometricPrimitiveAdapter;
import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
import com.github.ruediste.laserPcb.process.PrintPcbProcess.InputFileStatus;
import com.github.ruediste.laserPcb.process.PrintPcbProcess.PcbLayer;
import com.github.ruediste.laserPcb.process.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.PrintPcbProcessPMod.PrintPcbInputFilePMod;

@Service
@Scope("singleton")
public class ProcessAppController {
	private final Logger log = LoggerFactory.getLogger(ProcessAppController.class);

	@Autowired
	ProcessRepository repo;

	@Autowired
	FileUploadService fileUploadService;

	private ExecutorService executor = Executors.newCachedThreadPool();
	private Process process;
	private Object lock = new Object();

	@PostConstruct
	void init() {
		process = repo.get();
		if (process == null) {
			log.info("No persisted process found during initialization, creating new process");
			process = new Process();
			process.printPcb = new PrintPcbProcess();
			repo.set(process);
		}

		if (process.printPcb != null) {
			for (var file : process.printPcb.inputFiles) {
				file.status = InputFileStatus.PARSING;
				executor.submit(() -> parse(file));
			}
		}

		repo.set(process);
	}

	@PreDestroy
	void preDestroy() {
		executor.shutdownNow();
	}

	public ProcessPMod getPMod() {
		Process process = repo.get();
		ProcessPMod processPMod = new ProcessPMod();
		if (process.printPcb != null) {
			processPMod.printPcb = new PrintPcbProcessPMod();
			processPMod.printPcb.inputFiles = process.printPcb.inputFiles.stream().map(file -> {
				PrintPcbInputFilePMod filePMod = new PrintPcbInputFilePMod();
				filePMod.file = file;
				var data = imageData.get(file.id);
				if (data != null) {
					filePMod.svgAvailable = data.svg != null;
				}
				return filePMod;
			}).collect(toList());
		}
		return processPMod;
	}

	public void update(Consumer<Process> updater) {
		synchronized (lock) {
			updater.accept(process);
			repo.set(process);
		}
	}

	public void addFile(PrintPcbInputFile file) {
		file.status = InputFileStatus.PARSING;
		update(process -> process.printPcb.inputFiles.add(file));
		executor.submit(() -> parse(file));
	}

	private static class InputImageData {
		Geometry image;
		String svg;
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

			int targetWidth = 1000;
			double scale = targetWidth / bounds.getWidth();
			int targetHeight = (int) (bounds.getHeight() * scale);

			SVGGraphics2D svg = new SVGGraphics2D(targetWidth, targetHeight);
			svg.translate(0, targetHeight);
			svg.scale(scale, -scale);
			svg.translate(-bounds.getMinX(), -bounds.getMinY());

			svg.setColor(Color.black);
			svg.fill(imageShape);
			data.svg = svg.getSVGElement(null, false, new ViewBox(0, 0, targetWidth, targetHeight),
					PreserveAspectRatio.XMID_YMID, MeetOrSlice.MEET);
			synchronized (lock) {
				if (!process.printPcb.inputFiles.stream().anyMatch(f -> f.id.equals(file.id))) {
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
				repo.set(process);
			}
		} catch (Throwable t) {
			log.error("Error wile parsing {}", file.name, t);
			synchronized (lock) {
				file.status = InputFileStatus.ERROR;
				file.errorMessage = t.getMessage();
				repo.set(process);
			}
		}
	}

	public void removeFile(UUID id) {
		synchronized (lock) {
			if (process.printPcb != null) {
				imageData.remove(id);
				process.printPcb.inputFiles.removeIf(file -> file.id.equals(id));
				repo.set(process);
			}
		}
	}

	public String getSvg(UUID id) {
		InputImageData data = imageData.get(id);
		if (data == null)
			return null;
		return data.svg;
	}

}
