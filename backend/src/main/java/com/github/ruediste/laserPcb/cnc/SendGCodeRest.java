package com.github.ruediste.laserPcb.cnc;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ruediste.laserPcb.cnc.CncConnection.CncState;

@RestController
public class SendGCodeRest {
	private final static Logger log = LoggerFactory.getLogger(SendGCodeRest.class);

	public static class SendGCodeStatus {
		public List<String> lastCompletedGCodes;
		public List<String> inFlightGCodes;
		public List<String> nextGCodes;
		public CncState state;
	}

	@Autowired
	SendGCodeController ctrl;

	@Autowired
	CncConnectionAppController connCtrl;

	@GetMapping("sendGCode")
	public SendGCodeStatus status() {
		SendGCodeStatus s = new SendGCodeStatus();
		s.lastCompletedGCodes = ctrl.getLastCompletedGCodes(5);
		s.inFlightGCodes = ctrl.getInFlightGCodes();
		s.nextGCodes = ctrl.getNextGCdes(5);

		CncConnection con = connCtrl.getConnection();
		if (con != null) {
			s.state = con.getState();
		}
		return s;
	}

	@PostMapping("sendGCode/_cancel")
	public void cancel() {
		ctrl.cancel();
	}
}
