package com.github.marschwar.depvis;

public interface Logger {
	void info(String message);
	void error(String message);
	void error(String message, Throwable throwable);
}
