package com.github.marschwar.depvis;

public class SilentLogger implements Logger {
	public static final SilentLogger SILENT_LOGGER = new SilentLogger();
	@Override
	public void info(String message) {
		// noop
	}

	@Override
	public void error(String message) {
		// noop
	}

	@Override
	public void error(String message, Throwable throwable) {
		// noop
	}
}
