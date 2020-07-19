package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.Filters.FiltersBuilder;
import com.github.marschwar.classDependency.cytoscape.CytoscapeReportTransformer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command
public class GenerateReportCommand implements Callable<Integer> {

	@Parameters(
			paramLabel = "path",
			arity = "1",
			description = "can be a pathname to a .class file, a directory, a JAR file")
	private Path path;

	@Option(names = {"-i", "--includes"},
			paramLabel = "PATTERN",
			description = "pattern of types to include",
			defaultValue = ".*"
	)
	private Filter[] includes;
	@Option(names = {"-e", "--excludes"},
			paramLabel = "PATTERN",
			description = "pattern of types to exclude",
			defaultValue = "java.*"
	)
	private Filter[] excludes;

	@Override
	public Integer call() throws Exception {

		Logger logger = new StdoutLogger();
		FiltersBuilder filterBuilder = Filters.builder();
		for (Filter pattern : includes) {
			filterBuilder = filterBuilder.include(pattern);
		}
		for (Filter pattern : excludes) {
			filterBuilder = filterBuilder.exclude(pattern);
		}
		final ReportGenerator generator = ReportGenerator.builder()
				.filters(filterBuilder.build())
				.sourcePath(path)
				.logger(logger)
				.build();
		Report report;
		try {
			report = generator.generate();
		} catch (Exception e) {
			logger.error("Error generating report", e);
			return 1;
		}

		try (Writer writer = new BufferedWriter(new FileWriter("/tmp/deps.html"))) {
			new CytoscapeReportTransformer().transform(report, writer);
		} catch (IOException e) {
			System.err.println("Error writing report");
			e.printStackTrace();
			return 1;
		}
		return 0;
	}
}
