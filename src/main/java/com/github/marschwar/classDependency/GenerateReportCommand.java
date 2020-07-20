package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.Filters.FiltersBuilder;
import com.github.marschwar.classDependency.cytoscape.CytoscapeReportTransformer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

	@Option(
			names = {"-o", "--output-dir"},
			paramLabel = "DIR",
			description = "the path of the output directory")
	private Path outputDir;

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
		final Report report;
		try {
			report = generator.generate();
		} catch (ReportGenerationException e) {
			logger.error("Error generating report", e);
			return 1;
		}

		try (Writer writer = new BufferedWriter(getOutputWriter())) {
			new CytoscapeReportTransformer().transform(report, writer);
		} catch (IOException e) {
			logger.error("Error writing report", e);
			return 1;
		}
		return 0;
	}

	private Writer getOutputWriter() throws IOException {
		if (outputDir == null) {
			return new OutputStreamWriter(System.out);
		}

		final File targetDir = outputDir.toFile();
		targetDir.mkdirs();
		final File targetFile = new File(targetDir, "classDependencies.html");
		return new FileWriter(targetFile);
	}
}
