package com.github.marschwar.depvis;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.github.marschwar.depvis.cytoscape.CytoscapeReportTransformer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GenerateReportCommand implements IDefaultProvider {

	@Parameter(
			description = "can be a pathname to a .class file, a directory, a JAR file",
			converter = PathConverter.class,
			required = true
	)
	private Path path;

	@Parameter(names = {"-o", "--output-dir"},
			converter = PathConverter.class,
			description = "the path of the output directory")
	private Path outputDir;

	@Parameter(names = {"--includes", "-i"},
			converter = FilterConverter.class,
			description = "pattern of types to include"
	)
	private List<Filter> includes;

	@Parameter(names = {"--excludes", "-e"},
			converter = FilterConverter.class,
			description = "pattern of types to exclude"
	)
	private List<Filter> excludes;

	@Parameter(names = {"--cycles-only"},
			description = "include only classes that contain cycles"
	)
	private boolean cyclesOnly;

	@Parameter(names = {"--show-self-references"},
			description = "by default self references are not shown"
	)
	private boolean selfReferences;

	public void execute() throws ReportGenerationException {

		Logger logger = new StdoutLogger();
		Filters filters = Filters.builder()
				.include(includes)
				.exclude(excludes)
				.build();

		final ReportGenerator generator = ReportGenerator.builder()
				.sourcePath(path)
				.filters(filters)
				.cyclesOnly(cyclesOnly)
				.showSelfReferences(selfReferences)
				.logger(logger)
				.build();
		final Report report;
		report = generator.generate();

		final ReportTransformer transformer = createTransformer();
		try (Writer writer = getOutputWriter(transformer.getFileExtension())) {
			transformer.transform(report, writer);
		} catch (IOException e) {
			throw new ReportGenerationException("Error writing report", e);
		}
	}

	private ReportTransformer createTransformer() {
		return new CytoscapeReportTransformer();
	}

	private BufferedWriter getOutputWriter(String extension) throws IOException {
		if (outputDir == null) {
			return new BufferedWriter(new OutputStreamWriter(System.out));
		}

		Files.createDirectories(outputDir);
		final Path targetPath = outputDir.resolve("classDependencies." + extension);
		return Files.newBufferedWriter(targetPath);
	}

	@Override
	public String getDefaultValueFor(String optionName) {
		switch (optionName) {
			case "--includes":
				return ".*";
			case "--excludes":
				return "java.*";
			default:
				return null;
		}
	}

	private static class PathConverter implements IStringConverter<Path> {
		@Override
		public Path convert(String value) {
			return Paths.get(value);
		}
	}

	private static class FilterConverter implements IStringConverter<Filter> {
		@Override
		public Filter convert(String value) {
			return Filter.of(value);
		}
	}
}
