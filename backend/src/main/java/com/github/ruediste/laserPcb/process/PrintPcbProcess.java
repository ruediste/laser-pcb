package com.github.ruediste.laserPcb.process;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PrintPcbProcess {

	public enum PcbLayer {
		TOP, BOTTOM
	}

	public enum InputFileStatus {
		PARSING, ERROR, PARSED
	}

	public static class PrintPcbInputFile {
		public String name;
		public PcbLayer layer;
		public UUID id;
		public InputFileStatus status;
		public String errorMessage;
	}

	public List<PrintPcbInputFile> inputFiles = new ArrayList<>();
}
