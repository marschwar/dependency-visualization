package com.github.marschwar.classDependency;

public class ReportGenerationException extends Exception {
	public ReportGenerationException(String s) {
		super(s);
	}

	public ReportGenerationException(String s, Throwable throwable) {
		super(s, throwable);
	}
}
