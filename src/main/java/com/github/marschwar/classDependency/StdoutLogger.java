package com.github.marschwar.classDependency;

public class StdoutLogger implements Logger {
	@Override
	public void info(String message) {
		System.out.println(message);
	}

	@Override
	public void error(String message) {
		System.err.println(message);
	}

	@Override
	public void error(String message, Throwable throwable) {
		System.err.println(message);
		throwable.printStackTrace();
	}
}
