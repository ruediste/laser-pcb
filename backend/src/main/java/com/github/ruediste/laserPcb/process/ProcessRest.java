package com.github.ruediste.laserPcb.process;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcess;
import com.github.ruediste.laserPcb.process.cameraCalibration.CameraCalibrationProcessPMod;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationProcess;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationProcessPMod;
import com.github.ruediste.laserPcb.process.laserHeightCalibration.LaserHeightCalibrationService;
import com.github.ruediste.laserPcb.process.laserIntensityCalibration.LaserIntensityCalibrationProcess;
import com.github.ruediste.laserPcb.process.laserIntensityCalibration.LaserIntensityCalibrationProcessPMod;
import com.github.ruediste.laserPcb.process.laserIntensityCalibration.LaserIntensityCalibrationService;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.InputFileStatus;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PcbLayer;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcess.PrintPcbInputFile;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessController;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessPMod;
import com.github.ruediste.laserPcb.process.printPcb.PrintPcbProcessPMod.PrintPcbInputFilePMod;
import com.github.ruediste.laserPcb.profile.Profile;
import com.github.ruediste.laserPcb.profile.ProfileRepository;

@RestController
public class ProcessRest {
	private final Logger logger = LoggerFactory.getLogger(ProcessRest.class);

	@Autowired
	ProcessController ctrl;

	@Autowired
	ProcessRepository repo;

	@Autowired
	ProfileRepository profileRepo;

	@Autowired
	PrintPcbProcessController printPcbCtrl;

	@Autowired
	LaserHeightCalibrationService laserHeightCalibrationService;

	@Autowired
	LaserIntensityCalibrationService laserIntensityCalibrationService;

	@GetMapping("process")
	ProcessPMod getProcess() {
		Process process = repo.get();
		ProcessPMod processPMod = new ProcessPMod();
		Profile profile = profileRepo.getCurrent();
		PrintPcbProcess printPcb = process.printPcb;
		if (printPcb != null)
			processPMod.printPcb = toPMod(printPcb, profile);
		if (process.cameraCalibration != null)
			processPMod.cameraCalibration = toPMod(process.cameraCalibration);
		if (process.laserHeightCalibration != null)
			processPMod.laserHeightCalibration = toPMod(process.laserHeightCalibration);
		if (process.laserIntensityCalibration != null)
			processPMod.laserIntensityCalibration = toPMod(process.laserIntensityCalibration);
		return processPMod;
	}

	private PrintPcbProcessPMod toPMod(PrintPcbProcess printPcb, Profile profile) {
		PrintPcbProcessPMod printPcbPMod = new PrintPcbProcessPMod();
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
			printPcbPMod.readyToProcessFiles = printPcb.canStartProcessing(profile);
		}

		printPcbPMod.userMessage = printPcb.userMessage(profile);
		return printPcbPMod;
	}

	private CameraCalibrationProcessPMod toPMod(CameraCalibrationProcess process) {
		CameraCalibrationProcessPMod pMod = new CameraCalibrationProcessPMod();
		pMod.currentStep = process.currentStep;
		return pMod;
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

	private LaserHeightCalibrationProcessPMod toPMod(LaserHeightCalibrationProcess laserHeightCalibration) {
		LaserHeightCalibrationProcessPMod pMod = new LaserHeightCalibrationProcessPMod();
		pMod.currentStep = laserHeightCalibration.currentStep;
		pMod.laserHeights = laserHeightCalibrationService.getLaserHeights(laserHeightCalibration);
		return pMod;
	}

	private LaserIntensityCalibrationProcessPMod toPMod(LaserIntensityCalibrationProcess laserIntensityCalibration) {
		var pMod = new LaserIntensityCalibrationProcessPMod();
		pMod.currentStep = laserIntensityCalibration.currentStep;
		pMod.laserIntensities = laserIntensityCalibrationService.getLaserIntensities(laserIntensityCalibration);
		return pMod;
	}

}
