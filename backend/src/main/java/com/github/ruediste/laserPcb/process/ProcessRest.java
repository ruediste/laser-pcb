package com.github.ruediste.laserPcb.process;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.fileUpload.FileUploadService;
import com.github.ruediste.laserPcb.process.PrintPcbProcess.PrintPcbInputFile;

@RestController
public class ProcessRest {
	private final Logger logger = LoggerFactory.getLogger(ProcessRest.class);

	@Autowired
	ProcessAppController ctrl;

	@Autowired
	FileUploadService fileUploadService;

	@GetMapping("process")
	ProcessPMod getProcess() {
		return ctrl.getPMod();
	}

	@PostMapping(value = "process/_addFile", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public String addInputFile(@RequestParam String name, InputStream body) {
		UUID id = UUID.randomUUID();
		fileUploadService.store(id, body);
		PrintPcbInputFile file = new PrintPcbInputFile();
		file.name = name;
		file.id = id;
		ctrl.addFile(file);
		return id.toString();
	}

	@DeleteMapping(value = "process/file/{id}")
	public void removeInputFile(@PathVariable String id) {
		ctrl.removeFile(UUID.fromString(id));
	}

	@GetMapping(value = "process/file/svg/{id}.svg", produces = "image/svg+xml")
	public String getSvg(@PathVariable String id) {
		return ctrl.getSvg(UUID.fromString(id));
	}
}
