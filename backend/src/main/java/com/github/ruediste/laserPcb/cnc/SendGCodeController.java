package com.github.ruediste.laserPcb.cnc;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.ruediste.laserPcb.gCode.GCodeWriter;

@Component
public class SendGCodeController {
	private final static Logger log = LoggerFactory.getLogger(SendGCodeController.class);
	@Autowired
	CncConnectionAppController connCtrl;

	private class GCodeBlock {
		private List<String> gCodes;
		private int nextIndex;
		private volatile int completedIndex = -1;
		private CncConnection conn;
		private Consumer<Void> gCodeSender = x -> sendGCode();
		public boolean sending;
		public Runnable onComplete;

		public GCodeBlock(List<String> gCodes, Runnable onComplete) {
			this.gCodes = gCodes;
			this.onComplete = onComplete;
			conn = connCtrl.getConnection();
		}

		private void sendGCode() {
			synchronized (SendGCodeController.this) {
				if (!sending)
					return;

				while (nextIndex < gCodes.size()) {
					int idx = nextIndex;
					if (!conn.sendGCodeNonBlocking(gCodes.get(nextIndex), () -> completedIndex = idx)) {
						break;
					}
					nextIndex++;
				}

				log.info("completedIndex: {} nextIndex: {}, size: {}", completedIndex, nextIndex, gCodes.size());

				if (nextIndex >= gCodes.size()) {
					log.info("Sending Completed");
				}

				if (completedIndex >= gCodes.size() - 1) {
					log.info("Processing GCodes Completed");
					conn.gCodeCompleted.remove(gCodeSender);
					if (onComplete != null)
						onComplete.run();
					sending = false;
				}

			}
		}

		public void startSending() {
			sending = true;
			conn.gCodeCompleted.add(gCodeSender);
			sendGCode();
		}

		public boolean isComplete() {
			return completedIndex >= gCodes.size();
		}
	}

	private GCodeBlock sendingBlock;

	public synchronized boolean isComplete() {
		return sendingBlock == null || sendingBlock.isComplete();
	}

	public void sendGCodes(GCodeWriter gCodes) {
		sendGCodes(gCodes, null);
	}

	public void sendGCodes(GCodeWriter gCodes, Runnable onComplete) {
		sendGCodes(gCodes.getGCodes(), onComplete);
	}

	public void sendGCodes(List<String> gCodes) {
		sendGCodes(gCodes, null);
	}

	public synchronized void sendGCodes(List<String> gCodes, Runnable onComplete) {
		if (gCodes.isEmpty())
			return;

		sendingBlock = new GCodeBlock(gCodes, onComplete);
		sendingBlock.startSending();
	}

	public synchronized List<String> getLastCompletedGCodes(int n) {
		if (sendingBlock == null)
			return List.of();
		int to = sendingBlock.completedIndex + 1;
		int from = Math.max(0, to - n);
		return sendingBlock.gCodes.subList(from, to);
	}

	public synchronized List<String> getInFlightGCodes() {
		if (sendingBlock == null)
			return List.of();
		return sendingBlock.gCodes.subList(sendingBlock.completedIndex + 1, sendingBlock.nextIndex);
	}

	public synchronized List<String> getNextGCdes(int n) {
		if (sendingBlock == null)
			return List.of();
		int from = sendingBlock.nextIndex;
		int to = Math.min(sendingBlock.gCodes.size(), from + n);
		return sendingBlock.gCodes.subList(from, to);
	}
}
