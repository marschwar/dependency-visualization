package com.github.marschwar.classDependency;

import picocli.CommandLine;

public class Main {

	public static void main(String[] args) {
		final int exitCode = new CommandLine(new GenerateReportCommand())
				.registerConverter(Filter.class, Filter::of)
				.execute(args);
		System.exit(exitCode);
	}
}
