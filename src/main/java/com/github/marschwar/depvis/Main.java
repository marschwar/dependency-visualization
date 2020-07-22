package com.github.marschwar.depvis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {

	public static void main(String[] args) {
		final GenerateReportCommand command = new GenerateReportCommand();

		try {
			JCommander.newBuilder()
					.addObject(command)
					.defaultProvider(command)
					.build()
					.parse(args);
			command.execute();
			System.exit(0);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			System.exit(2);
		} catch (ReportGenerationException e) {
			System.exit(1);
		}

	}
}
