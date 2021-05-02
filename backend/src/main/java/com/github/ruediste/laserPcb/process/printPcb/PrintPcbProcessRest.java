package com.github.ruediste.laserPcb.process.printPcb;

import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
import com.github.ruediste.laserPcb.process.ProcessController;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessController.InputFileData;

@RestController
public class PrintPcbProcessRest {
	private final Logger logger = LoggerFactory.getLogger(PrintPcbProcessRest.class);

	@Autowired
	PrintPcbProcessController ctrl;

	@Autowired
	ProcessController processCtrl;

	@Autowired
	FileUploadService fileUploadService;

	@PostMapping(value = "process/printPcb/_addFile", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public String addInputFile(@RequestParam String name, InputStream body) {
		UUID id = UUID.randomUUID();
		fileUploadService.store(id, body);
		PrintPcbInputFile file = new PrintPcbInputFile();
		file.name = name;
		file.id = id;
		ctrl.addFile(file);
		return id.toString();
	}

	@DeleteMapping(value = "process/printPcb/file/{id}")
	public void removeInputFile(@PathVariable String id) {
		ctrl.removeFile(UUID.fromString(id));
	}

	@PostMapping(value = "process/printPcb/launch")
	public void launch() {
		processCtrl.update(p -> {
			if (p.printPcb == null) {
				p.clear();
				p.printPcb = new PrintPcbProcess();
			}
		});
	}

	@PostMapping(value = "process/printPcb/file/{id}")
	public void updateFile(@PathVariable String id, @RequestBody PrintPcbInputFile file) {
		ctrl.update(p -> {
			p.getFile(UUID.fromString(id)).layer = file.layer;
		});
	}

	@GetMapping(value = "process/printPcb/file/{id}/input{hash}.svg", produces = "image/svg+xml")
	public String getInputSvg(@PathVariable String id, @PathVariable String hash) {
		InputFileData imageData = ctrl.getImageData(UUID.fromString(id));
		if (!imageData.inputSvgHash.equals(hash))
			throw new RuntimeException("Hash mismatch");
		return imageData.inputSvg;
	}

	@GetMapping(value = "process/printPcb/file/{id}/image{hash}.svg", produces = "image/svg+xml")
	public String getImageSvg(@PathVariable String id, @PathVariable String hash) {
		InputFileData imageData = ctrl.getImageData(UUID.fromString(id));
		if (!imageData.imageSvgHash.equals(hash))
			throw new RuntimeException("Hash mismatch");
		return imageData.imageSvg;
	}

	@GetMapping(value = "process/printPcb/file/{id}/buffers{hash}.svg", produces = "image/svg+xml")
	public String getBuffersSvg(@PathVariable String id, @PathVariable String hash) {
		InputFileData imageData = ctrl.getImageData(UUID.fromString(id));
		if (!imageData.buffersSvgHash.equals(hash))
			throw new RuntimeException("Hash mismatch");
		return imageData.buffersSvg;
	}

	@PostMapping("process/printPcb/_processFiles")
	public void processFiles() {
		ctrl.startProcessFiles();
	}

	@PostMapping("process/printPcb/_startExposing")
	public void startExposing() {
		ctrl.startExposing();
	}

	@PostMapping("process/printPcb/_addPositionPoint")
	public void addPositionPoint() {
		ctrl.addPositionPoint();
	}
}
