package org.proofpad;

import java.io.File;

public class FileMonitor extends Thread {

	private final File file;
	private final Runnable callback;
	private boolean running;
	private long mtime;

	public FileMonitor(File file, Runnable callback) {
		this.file = file;
		this.callback = callback;
		mtime = -1;
		running = true;
	}
	
	@Override public void run() {
		while (running) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }
			if (mtime == -1) {
				mtime = file.lastModified();
			} else if (file.lastModified() != mtime) {
				callback.run();
				mtime = file.lastModified();
			}
		}
	}
	
	public void requestStop() {
		running = false;
	}

	public void ignoreOne() {
		mtime = -1;
	}
}
