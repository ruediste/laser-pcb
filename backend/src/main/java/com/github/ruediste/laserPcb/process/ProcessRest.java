package com.github.ruediste.laserPcb.process;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.InputFileStatus;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PcbLayer;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessAppController;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessPMod;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessPMod.PrintPcbInputFilePMod;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@RestController
public class ProcessRest {
	private final Logger logger = LoggerFactory.getLogger(ProcessRest.class);

	@Autowired
	ProcessAppController ctrl;

	@Autowired
	ProcessRepository repo;

	@Autowired
	ProfileRepository profileRepo;

	@Autowired
	PrintPcbProcessAppController printPcbCtrl;

	@GetMapping("process")
	ProcessPMod getProcess() {
		Process process = repo.get();
		ProcessPMod processPMod = new ProcessPMod();
		Profile profile = profileRepo.getCurrent();
		PrintPcbProcess printPcb = process.printPcb;
		if (printPcb != null) {
			PrintPcbProcessPMod printPcbPMod = new PrintPcbProcessPMod();
			processPMod.printPcb = printPcbPMod;
			printPcbPMod.status = printPcb.status;
			for (var file : printPcb.inputFiles) {
				printPcbPMod.inputFiles.add(toPMod(file));
			}

			Map<PcbLayer, PrintPcbInputFile> fileMap = printPcb.fileMap();
			for (var layer : PcbLayer.values()) {
				PrintPcbInputFile file = fileMap.get(layer);
				if (file == null || file.status != InputFileStatus.PROCESSED)
					continue;
				printPcbPMod.processedFiles.add(toPMod(file));
			}

			if (profile != null) {
				processPMod.printPcb.readyToProcessFiles = printPcb.canStartProcessing(profile);
			}
		}
		return processPMod;
	}

	private PrintPcbInputFilePMod toPMod(PrintPcbInputFile file) {
		PrintPcbInputFilePMod filePMod = new PrintPcbInputFilePMod();
		filePMod.file = file;
		var data = printPcbCtrl.getImageData(file.id);
		if (data != null) {
			filePMod.inputSvgAvailable = data.inputSvg != null;
			filePMod.inputSvgHash = data.inputSvgHash;
			filePMod.imageSvgAvailable = data.imageSvg != null;
			filePMod.imageSvgHash = data.imageSvgHash;
			filePMod.buffersSvgAvailable = data.buffersSvg != null;
			filePMod.buffersSvgHash = data.buffersSvgHash;

		}
		return filePMod;
	}

}
