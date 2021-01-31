package com.github.ruediste.laserPcb;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RepositoryBase<T extends EntityBase> {

	private Pattern filePattern = Pattern
			.compile("(?<id>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.json");

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	DataDirService dataDirService;

	Map<UUID, byte[]> serializedValues = new HashMap<>();
	Map<UUID, T> values = new HashMap<>();

	private Class<T> cls;

	private Path dataDir;

	@SuppressWarnings({ "unchecked" })
	@PostConstruct
	private void init() {
		cls = (Class<T>) getFirstSuperClassTypeParameter(getClass());
		dataDir = dataDirService.getDataDir().resolve(getSimpleNameLowerCamel(cls));
		try {
			Files.createDirectories(dataDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		reload();
	}

	static String getSimpleNameLowerCamel(Class<?> cls) {
		return cls.getSimpleName().substring(0, 1).toLowerCase() + cls.getSimpleName().substring(1);
	}

	static Class<?> getFirstSuperClassTypeParameter(Class<?> startCls) {
		ParameterizedType t = (ParameterizedType) startCls.getGenericSuperclass();
		return (Class<?>) t.getActualTypeArguments()[0];
	}

	/**
	 * Reload all instances from the file system.
	 */
	synchronized public void reload() {
		try {
			Files.list(dataDir).forEach(file -> {
				try {
					if (file.toString().equals("tmp.json"))
						Files.delete(file);
					Matcher matcher = filePattern.matcher(file.getFileName().toString().toLowerCase(Locale.ENGLISH));
					if (matcher.matches()) {
						byte[] bb = Files.readAllBytes(file);
						UUID id = UUID.fromString(matcher.group("id"));
						T value = objectMapper.readValue(bb, cls);
						serializedValues.put(id, bb);
						values.put(id, value);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	synchronized public UUID save(T value) {
		if (value.id == null)
			value.id = UUID.randomUUID();
		try (var out = new ByteArrayOutputStream()) {
			objectMapper.writeValue(out, value);
			byte[] bb = out.toByteArray();
			Path tmpPath = dataDir.resolve("tmp.json");
			Files.write(tmpPath, bb);
			Files.move(tmpPath, dataDir.resolve(value.id + ".json"), StandardCopyOption.REPLACE_EXISTING);
			serializedValues.put(value.id, bb);
			values.put(value.id, objectMapper.readValue(bb, cls));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return value.id;
	}

	/**
	 * Return a copy of the instance with the given id, or null if no object is
	 * found
	 */
	synchronized public T get(UUID id) {
		try {
			byte[] src = serializedValues.get(id);
			if (src == null)
				return null;
			return objectMapper.readValue(src, cls);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Apply a test to all instances and return a copy of the matching instance. If
	 * multiple instances match, an error is thrown.
	 */
	synchronized public T getSingle(Predicate<T> test) {
		List<Entry<UUID, T>> matching = values.entrySet().stream().filter(e -> test.test(e.getValue()))
				.collect(toList());
		if (matching.size() > 1)
			throw new RuntimeException("Multiple matching values found");
		if (matching.isEmpty())
			return null;
		return get(matching.get(0).getKey());
	}

	/**
	 * Apply a test to all instances and return a copy of all matching instances
	 */
	synchronized public List<T> getAll(Predicate<T> test) {
		return values.entrySet().stream().filter(e -> test.test(e.getValue())).map(e -> get(e.getKey()))
				.collect(toList());
	}

	synchronized public List<T> getAll() {
		return values.entrySet().stream().map(e -> get(e.getKey())).collect(toList());
	}

	public void delete(UUID id) {
		try {
			Files.delete(dataDir.resolve(id.toString() + ".json"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		values.remove(id);
		serializedValues.remove(id);
	}
}
