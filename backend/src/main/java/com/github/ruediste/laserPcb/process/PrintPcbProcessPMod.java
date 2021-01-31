package com.github.ruediste.laserPcb.process;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.laserPcb.process.PrintPcbProcess.PrintPcbInputFile;

public class PrintPcbProcessPMod {

	public static class PrintPcbInputFilePMod {
		public PrintPcbInputFile file;
		public boolean svgAvailable;
	}

	public List<PrintPcbInputFilePMod> inputFiles = new ArrayList<>();
}
