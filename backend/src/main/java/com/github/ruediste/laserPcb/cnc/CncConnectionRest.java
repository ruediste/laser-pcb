package com.github.ruediste.laserPcb.cnc;

import static java.util.stream.Collectors.toList;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CncConnectionRest {

	@Autowired
	AvailableDevicesAppController ctrl;

	@Autowired
	SelectedSerialConnectionRepository selectedSerialRepo;

	@Autowired
	VideoAppController videoCtrl;

	@Autowired
	CncConnectionAppController connCtrl;

	public static class CncConnectionState {
		public String selectedSerialConnection;
		public List<String> availableSerialConnections;
		public boolean serialConnected;

		public Double x;
		public Double y;
		public Double z;

		public String selectedVideoConnection;
		public List<String> availableVideoConnections;
		public boolean videoConnected;
	}

	@GetMapping("cncConnection")
	CncConnectionState cncConnectionState() {
		var result = new CncConnectionState();
		CncConnection connection = connCtrl.getConnection();

		result.serialConnected = connection != null;
		result.selectedSerialConnection = selectedSerialRepo.get();
		result.availableSerialConnections = ctrl.getCurrentSerialConnections().stream().map(x -> x.toString()).sorted()
				.collect(toList());

		if (connection != null) {
			var state = connection.getState();
			result.x = state.x;
			result.y = state.y;
			result.z = state.z;
		}

		result.selectedVideoConnection = videoCtrl.getVideoDevice();
		result.availableVideoConnections = ctrl.getCurrentVideoConnections().stream().map(x -> x.toString()).sorted()
				.collect(toList());

		return result;
	}

	@PostMapping("cncConnection/_setSerialConnection")
	void setCurrentSerialConnection(@RequestParam String dev) {
		selectedSerialRepo.set(dev);
	}

	@PostMapping("cncConnection/_setVideoConnection")
	void setCurrentVideoConnection(@RequestParam String dev) {
		videoCtrl.connect(dev);
	}

	@GetMapping(value = "cncConnection/frame.jpg", produces = MimeTypeUtils.IMAGE_JPEG_VALUE)
	byte[] getFrame() {
		Object lock = videoCtrl.getLock();
		synchronized (lock) {
			try {
				lock.wait(500);
			} catch (InterruptedException e) {
				Thread.interrupted();
				return null;
			}
		}
		return videoCtrl.getCurrentFrame();
	}

	@PostMapping("cncConnection/_connect")
	void connect() {
		connCtrl.connect(Paths.get(selectedSerialRepo.get()));
	}

	@PostMapping("cncConnection/_disconnect")
	void disconnect() {
		connCtrl.disconnect();
	}

	@PostMapping("cncConnection/_jog")
	void jog(@RequestParam String direction) {
		if ("X-".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 X-1");
		if ("X+".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 X1");

		if ("Y-".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 Y-1");
		if ("Y+".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 Y1");

		if ("Z-".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 Z-1");
		if ("Z+".equals(direction))
			connCtrl.getConnection().sendGCodeForJog("G0 Z1");
	}

	@PostMapping("cncConnection/_autoHome")
	void autoHome() {
		connCtrl.getConnection().sendGCode("G28");
	}
}
