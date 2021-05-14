package com.github.ruediste.laserPcb.cnc;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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
		public boolean cancelled;
		private CompletableFuture<Void> future;

		public GCodeBlock(List<String> gCodes, CompletableFuture<Void> future) {
			this.gCodes = gCodes;
			this.future = future;
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

				boolean completed = false;
				if (cancelled) {
					log.info("Processing GCodes cancelled");
					future.cancel(true);
					completed = true;
				} else if (completedIndex >= gCodes.size() - 1) {
					log.info("Processing GCodes completed");
					future.complete(null);
					completed = true;
				}

				if (completed) {
					conn.gCodeCompleted.remove(gCodeSender);
					sending = false;
					// connCtrl.setStatusQueryEnabled(true);
				}

			}
		}

		public void startSending() {
			sending = true;
			// connCtrl.setStatusQueryEnabled(false);
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

	public CompletableFuture<Void> sendGCodes(GCodeWriter gCodes) {
		return sendGCodes(gCodes.getGCodes());
	}

	public synchronized CompletableFuture<Void> sendGCodes(List<String> gCodes) {
		if (gCodes.isEmpty())
			return CompletableFuture.completedFuture(null);

		CompletableFuture<Void> future = new CompletableFuture<>();
		sendingBlock = new GCodeBlock(gCodes, future);
		sendingBlock.startSending();
		return future;
	}

	public synchronized List<String> getLastCompletedGCodes(int n) {
		if (sendingBlock == null)
			return List.of();
		int to = sendingBlock.completedIndex + 1;
		int from = Math.max(0, to - n);
		return sendingBlock.gCodes.subList(from, to);
	}

	public synchronized List<String> getInFlightGCodes() {
		if (sendingBlock == null || sendingBlock.sending == false)
			return List.of();
		return sendingBlock.gCodes.subList(sendingBlock.completedIndex + 1, sendingBlock.nextIndex);
	}

	public synchronized List<String> getNextGCdes(int n) {
		if (sendingBlock == null || sendingBlock.sending == false)
			return List.of();
		int from = sendingBlock.nextIndex;
		int to = Math.min(sendingBlock.gCodes.size(), from + n);
		return sendingBlock.gCodes.subList(from, to);
	}

	public synchronized void cancel() {
		sendingBlock.cancelled = true;
	}
}
