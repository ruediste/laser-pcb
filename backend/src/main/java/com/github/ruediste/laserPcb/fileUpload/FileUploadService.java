package com.github.ruediste.laserPcb.fileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.ruediste.laserPcb.DataDirService;

@Service
public class FileUploadService {

	@Autowired
	DataDirService dataDirService;

	public Path getPath(UUID id) {
		Path filesDir = filesDir();
		try {
			Files.createDirectories(filesDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return filesDir.resolve(id.toString() + ".dat");
	}

	private Path filesDir() {
		return dataDirService.getDataDir().resolve("files");
	}

	public void clear() {
		Path filesDir = filesDir();
		try {
			Files.delete(filesDir);
			Files.createDirectories(filesDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void store(UUID id, InputStream stream) {
		try {
			Files.copy(stream, getPath(id), StandardCopyOption.REPLACE_EXISTING);
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
