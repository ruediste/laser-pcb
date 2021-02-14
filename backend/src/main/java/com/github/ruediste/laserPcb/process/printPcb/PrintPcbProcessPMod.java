package com.github.ruediste.laserPcb.process.printPcb;

import java.util.ArrayList;
import java.util.List;

import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbStatus;

public class PrintPcbProcessPMod {

	public static class PrintPcbInputFilePMod {
		public PrintPcbInputFile file;
		public boolean inputSvgAvailable;
		public boolean imageSvgAvailable;
		public boolean buffersSvgAvailable;
		public String inputSvgHash;
		public String imageSvgHash;
		public String buffersSvgHash;
	}

	public PrintPcbStatus status;
	public List<PrintPcbInputFilePMod> inputFiles = new ArrayList<>();
	public List<PrintPcbInputFilePMod> processedFiles = new ArrayList<>();

	public boolean readyToProcessFiles;
}
