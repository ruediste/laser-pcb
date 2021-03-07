package com.github.ruediste.laserPcb.jni;

import java.io.File;

public class HelloWorldJNI {

	static {
		System.setProperty("java.library.path", new File("target/obj").getAbsolutePath());
		System.loadLibrary("native");
	}

	public static void main(String[] args) {
		new HelloWorldJNI().sayHello();
	}

	// Declare a native method sayHello() that receives no arguments and returns
	// void
	private native void sayHello();
}