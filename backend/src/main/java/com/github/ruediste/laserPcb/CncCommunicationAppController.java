package com.github.ruediste.laserPcb;

import static jtermios.JTermios.B115200;
import static jtermios.JTermios.CLOCAL;
import static jtermios.JTermios.CREAD;
import static jtermios.JTermios.CS8;
import static jtermios.JTermios.CSIZE;
import static jtermios.JTermios.CSTOPB;
import static jtermios.JTermios.ECHO;
import static jtermios.JTermios.ECHOE;
import static jtermios.JTermios.FD_SET;
import static jtermios.JTermios.FD_ZERO;
import static jtermios.JTermios.F_SETFL;
import static jtermios.JTermios.ICANON;
import static jtermios.JTermios.INPCK;
import static jtermios.JTermios.ISIG;
import static jtermios.JTermios.IXANY;
import static jtermios.JTermios.IXOFF;
import static jtermios.JTermios.IXON;
import static jtermios.JTermios.OPOST;
import static jtermios.JTermios.O_NOCTTY;
import static jtermios.JTermios.O_NONBLOCK;
import static jtermios.JTermios.O_RDWR;
import static jtermios.JTermios.PARENB;
import static jtermios.JTermios.TCIOFLUSH;
import static jtermios.JTermios.TCSANOW;
import static jtermios.JTermios.VMIN;
import static jtermios.JTermios.VTIME;
import static jtermios.JTermios.cfsetispeed;
import static jtermios.JTermios.cfsetospeed;
import static jtermios.JTermios.fcntl;
import static jtermios.JTermios.newFDSet;
import static jtermios.JTermios.open;
import static jtermios.JTermios.read;
import static jtermios.JTermios.select;
import static jtermios.JTermios.tcflush;
import static jtermios.JTermios.tcgetattr;
import static jtermios.JTermios.tcsetattr;
import static jtermios.JTermios.write;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jtermios.JTermios;
import jtermios.JTermios.FDSet;
import jtermios.Termios;
import jtermios.TimeVal;

@Service
@Scope("singleton")
public class CncCommunicationAppController {

	private final Logger logger = LoggerFactory.getLogger(CncCommunicationAppController.class);

	private int fd;

	private Thread readerThread;

	private volatile boolean closing;
	private CountDownLatch closed = new CountDownLatch(1);

//	@PostConstruct
	public void init() {
		String port = "/dev/ttyUSB0";
		logger.info("Opening {}", port);
		fd = open(port, O_RDWR | O_NOCTTY | O_NONBLOCK);
		if (fd == -1)
			throw new RuntimeException("Could not open " + port);

		fcntl(fd, F_SETFL, 0);

		Termios opts = new Termios();
		tcgetattr(fd, opts);
		opts.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
		opts.c_cflag |= (CLOCAL | CREAD);
		opts.c_cflag &= ~PARENB; // PARENB: Enable parity generation on output and parity checking for input
		opts.c_cflag |= CSTOPB;
		opts.c_cflag &= ~CSIZE;
		opts.c_cflag |= CS8;
		opts.c_oflag &= ~OPOST;
		opts.c_iflag &= ~INPCK;
		opts.c_iflag &= ~(IXON | IXOFF | IXANY);
		opts.c_cc[VMIN] = 0;
		opts.c_cc[VTIME] = 10;

		cfsetispeed(opts, B115200);
		cfsetospeed(opts, B115200);

		tcsetattr(fd, TCSANOW, opts);
		tcflush(fd, TCIOFLUSH);

		logger.info("Port {} opened", port);

		readerThread = new Thread(this::readLoop, "serialReader");
		readerThread.start();

		out = new PipedOutputStream();
		try {
			in = new PipedInputStream(out, 1024);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

//		new Thread(() -> {
//			try {
//				while (true) {
//					set(0, 0);
//					Thread.sleep(1000);
//					set(0, 255);
//					Thread.sleep(1000);
//				}
//			} catch (Throwable t) {
//				logger.error("error", t);
//			}
//		}).start();
	}

	PipedOutputStream out;
	PipedInputStream in;

	private void readLoop() {
		logger.info("Starting to read from serial port");
		try {
			FDSet rdset = newFDSet();
			FD_ZERO(rdset);
			FD_SET(fd, rdset);

			TimeVal tout = new TimeVal();
			tout.tv_sec = 0;
			tout.tv_usec = TimeUnit.MICROSECONDS.convert(Duration.ofMillis(100));

			byte buffer[] = new byte[1024];
			while (true) {
				int s = select(fd + 1, rdset, null, null, tout);
				if (s < 0) {
					throw new RuntimeException("select() failed ");
				}
				int m = read(fd, buffer, buffer.length);
				if (m < 0) {
					throw new RuntimeException("read() failed ");
				}
				if (m > 0) {
					logger.info("received {}", hexDump(buffer, 0, m));
					out.write(buffer, 0, m);
				}
				if (closing) {
					break;
				}
			}
		} catch (Throwable t) {
			logger.error("Error in read loop", t);
		}
		closed.countDown();
		logger.info("reading from serial port stopped");
	}

	private static String hexChars = "0123456789ABCDEF";

	private String hexDump(byte[] buffer, int offset, int len) {
		StringBuilder sbHex = new StringBuilder();
		StringBuilder sbAscii = new StringBuilder();
		for (int idx = offset; idx < offset + len; idx++) {
			byte ch = buffer[idx];
			sbHex.append(hexChars.charAt((ch >> 4) & 0xF));
			sbHex.append(hexChars.charAt(ch & 0xF));
			sbHex.append(" ");
			if (ch > 32)
				sbAscii.append((char) ch);
			else
				sbAscii.append(' ');
		}
		return sbHex.toString() + "   " + sbAscii.toString();
	}

//	@PreDestroy
	public void close() {
		closing = true;
		try {
			closed.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new RuntimeException(e);
		}
		int ec = JTermios.close(fd);
	}

	public void set(int channel, int value) {
		logger.info("Setting channel {} to {}", channel, value);
		byte[] tx = new byte[2];
		tx[0] = (byte) (0b0000 + channel);
		tx[1] = (byte) value;
		sendBytes(tx);
	}

	public void sendBytes(byte[] tx) {
		int l = 0;
		while (true) {
			int n = write(fd, Arrays.copyOfRange(tx, l, tx.length), tx.length - l);
			if (n < 0) {
				throw new RuntimeException("write() failed ");
			}
			l += n;
			if (l >= tx.length)
				break;
		}
		logger.info("Sending {} complete", hexDump(tx, 0, tx.length));
	}

}
