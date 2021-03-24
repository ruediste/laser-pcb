package com.github.ruediste.laserPcb.cnc;

import static java.util.stream.Collectors.toList;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CncConnectionRest {

	@Autowired
	AvailableSerialConnectionsAppController ctrl;

	@Autowired
	SelectedSerialConnectionRepository selectedSerialRepo;

	@Autowired
	CncConnectionAppController connCtrl;

	public static class CncConnectionState {
		public String selectedSerialConnection;
		public List<String> availableSerialConnections;
		public boolean connected;

		public Double x;
		public Double y;
		public Double z;
	}

	@GetMapping("cncConnection")
	CncConnectionState getSerialConnections() {
		var result = new CncConnectionState();
		CncConnection connection = connCtrl.getConnection();
		result.connected = connection != null;
		result.selectedSerialConnection = selectedSerialRepo.get();
		result.availableSerialConnections = result.availableSerialConnections = ctrl.getCurrentSerialConnections()
				.stream().map(x -> x.toAbsolutePath().toString()).sorted().collect(toList());
		if (connection != null) {
			var state = connection.getState();
			result.x = state.x;
			result.y = state.y;
			result.z = state.z;
		}
		return result;
	}

	@PostMapping("cncConnection/_setSerialConnection")
	void setCurrentConnection(@RequestParam String dev) {
		selectedSerialRepo.set(dev);
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
		connCtrl.ensureJogging();
		if ("X+".equals(direction))
			connCtrl.getConnection().sendGCode("G0 X1");
	}

	@PostMapping("cncConnection/_autoHome")
	void autoHome() {
		connCtrl.getConnection().sendGCode("G28");
	}
}
