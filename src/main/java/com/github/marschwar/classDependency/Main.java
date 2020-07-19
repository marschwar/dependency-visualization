package com.github.marschwar.classDependency;

import picocli.CommandLine;

public class Main {

	public static void main(String[] args) {
		final int exitCode = new CommandLine(new GenerateReportCommand()).execute(args);
		System.exit(exitCode);
	}
}
