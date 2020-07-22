package com.github.marschwar.depvis;

public class ReportGenerationException extends Exception {
	public ReportGenerationException(String s) {
		super(s);
	}

	public ReportGenerationException(String s, Throwable throwable) {
		super(s, throwable);
	}
}
