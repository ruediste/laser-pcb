package com.github.ruediste.laserPcb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SingletonRepositoryBase<T> {

	@Autowired
	DataDirService dataDirService;
	@Autowired
	ObjectMapper objectMapper;

	private byte[] currentValue;

	private Path path;

	private Class<T> cls;

	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		String name = getClass().getSimpleName();
		if (name.endsWith("Repository"))
			name = name.substring(0, name.length() - "Repository".length());
		name = name.substring(0, 1).toLowerCase() + name.substring(1);
		path = dataDirService.getDataDir().resolve(name);
		cls = (Class<T>) RepositoryBase.getFirstSuperClassTypeParameter(getClass());
		reload();
	}

	synchronized public void reload() {
		if (Files.exists(path)) {
			try {
				currentValue = Files.readAllBytes(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else
			currentValue = null;
	}

	/**
	 * Return a fresh copy of the current value
	 */
	synchronized public T get() {
		if (currentValue == null)
			return null;
		try {
			return objectMapper.readValue(currentValue, cls);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	synchronized public void set(T value) {
		try {
			if (value == null) {
				Files.delete(path);
				currentValue = null;
			} else {
				var tmp = objectMapper.writeValueAsBytes(value);
				Files.write(path, tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				currentValue = tmp;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
