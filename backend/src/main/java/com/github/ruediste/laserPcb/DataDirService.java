package com.github.ruediste.laserPcb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataDirService {

	@Autowired
	private void init() {
		try {
			Files.createDirectories(getDataDir());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getDataDir() {
		return Paths.get("/home/ruedi/Dropbox/laserPcb/");
	}
}
