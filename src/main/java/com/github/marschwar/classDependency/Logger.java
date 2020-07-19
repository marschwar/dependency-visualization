package com.github.marschwar.classDependency;

public interface Logger {
	void info(String message);
	void error(String message);
	void error(String message, Throwable throwable);
}
