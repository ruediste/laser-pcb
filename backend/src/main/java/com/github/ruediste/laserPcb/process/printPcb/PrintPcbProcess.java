package com.github.ruediste.laserPcb.process.printPcb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.ruediste.laserPcb.profile.Profile;

public class PrintPcbProcess {

	public enum PcbLayer {
		TOP, BOTTOM
	}

	public enum PrintPcbStatus {
		INITIAL(true), PROCESSING_FILES(false), EXPOSING(true);

		public final boolean canStartProcessing;

		private PrintPcbStatus(boolean canStartProcessing) {
			this.canStartProcessing = canStartProcessing;
		}
	}

	public PrintPcbStatus status = PrintPcbStatus.INITIAL;

	public enum InputFileStatus {
		PARSING(false), ERROR_PARSING(false), PARSED(true), PROCESSING(false), PROCESSED(true), ERROR_PROCESSING(true);

		public final boolean canStartProcessing;

		private InputFileStatus(boolean canStartProcessing) {
			this.canStartProcessing = canStartProcessing;
		}
	}

	public static class PrintPcbInputFile {
		public String name;
		public PcbLayer layer;
		public UUID id;
		public InputFileStatus status;
		public String errorMessage;
	}

	public List<PrintPcbInputFile> inputFiles = new ArrayList<>();

	public boolean canStartProcessing(Profile profile) {
		if (!status.canStartProcessing)
			return false;

		Map<PcbLayer, PrintPcbInputFile> fileMap = fileMap();

		if (profile.singleLayerPcb)
			return fileMap.size() == 1;
		else
			return fileMap.size() == 2;
	}

	public Map<PcbLayer, PrintPcbInputFile> fileMap() {
		Map<PcbLayer, List<PrintPcbInputFile>> fileMap = new HashMap<>();

		for (var file : inputFiles) {
			if (!file.status.canStartProcessing)
				continue;
			if (file.layer == null)
				continue;
			fileMap.computeIfAbsent(file.layer, x -> new ArrayList<>()).add(file);
		}
		if (fileMap.values().stream().anyMatch(x -> x.size() != 1))
			return null;
		Map<PcbLayer, PrintPcbInputFile> result = new HashMap<>();
		fileMap.forEach((k, v) -> result.put(k, v.get(0)));
		return result;
	}

	public PrintPcbInputFile getFile(UUID id) {
		for (var file : inputFiles) {
			if (file.id.equals(id))
				return file;
		}
		throw new RuntimeException("No file with id " + id + " found");
	}
}
