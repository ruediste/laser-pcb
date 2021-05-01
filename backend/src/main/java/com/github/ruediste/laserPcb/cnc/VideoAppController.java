package com.github.ruediste.laserPcb.cnc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.util.Strings;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VideoAppController {
	private final Logger log = LoggerFactory.getLogger(VideoAppController.class);

	@Autowired
	SelectedVideoConnectionRepository selectedVideoRepo;

	private volatile boolean closed;

	private volatile String device;

	private volatile byte[] currentFrame;

	private Object lock = new Object();

	@PostConstruct
	public void postConstruct() {
		device = selectedVideoRepo.get();
		Thread th = new Thread(this::videoLoop, "VideoLoop");
		th.start();
	}

	@PreDestroy
	public void preDestroy() {
		closed = true;
	}

	public void connect(String videoDevice) {
		selectedVideoRepo.set(videoDevice);
		device = videoDevice;
	}

	public String getVideoDevice() {
		return device;
	}

	public byte[] getCurrentFrame() {
		return currentFrame;
	}

	public Object getLock() {
		return lock;
	}

	private void videoLoop() {
		// outer loop: watch for video device
		while (!closed) {
			try {
				String d = device;
				if (Strings.isEmpty(d)) {
					Thread.sleep(100);
					continue;
				}
				FrameGrabber grabber;
				try {
					grabber = new OpenCVFrameGrabber(d); // 1 for next camera
					grabber.start();
				} catch (Throwable t) {
					log.error("Error while connecting to video device {}", d, t);
					Thread.sleep(1000);
					continue;

				}

				try {
					// inner loop: grab frames
					log.info("connected to {}", device);
					BufferedImage bImg = new BufferedImage(grabber.getImageWidth(), grabber.getImageHeight(),
							BufferedImage.TYPE_3BYTE_BGR);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					while (!closed && d.equals(device)) {
						Frame img = grabber.grab();
						if (img != null) {
							Java2DFrameConverter.copy(img, bImg);
							ImageIO.write(bImg, "jpg", baos);
							currentFrame = baos.toByteArray();
							synchronized (lock) {
								lock.notifyAll();
							}
							baos.reset();
							Thread.sleep(10000);
						}
					}

				} finally {
					log.info("disconnecting from {}", device);
					grabber.close();
				}

			} catch (Throwable t) {
				log.error("Error in video Loop", t);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
