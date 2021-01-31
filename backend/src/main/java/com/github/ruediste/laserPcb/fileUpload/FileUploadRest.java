package com.github.ruediste.laserPcb.fileUpload;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileUploadRest {

	@Autowired
	FileUploadService service;

	@PostMapping(value = "file", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public String uploadFile(@RequestParam String name, InputStream body) {
		UUID id = UUID.randomUUID();
		service.store(id, body);
		return id.toString();
	}
}
