package com.github.marschwar.depvis;

import com.beust.jcommander.IDefaultProvider;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.marschwar.depvis.cytoscape.CytoscapeReportTransformer;
import com.github.marschwar.depvis.dot.DotReportTransformer;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Parameters(commandDescription = "Generate a visualization of class dependencies")
public class GenerateReportCommand implements IDefaultProvider {

	@Getter
	@Parameter(names = "--help", help = true, description = "Show all available options", order = 0)
	private boolean help;

	@Parameter(
			description = "<directory or jar file>",
			converter = PathConverter.class,
			required = true
	)
	private Path path;

	@Parameter(order = 1,
			names = {"--output-dir", "-o"},
			converter = PathConverter.class,
			description = "The output directory in which to store the generated output file. " +
					"If not specified, the output is written to STDOUT.")
	private Path outputDir;

	@Parameter(order = 2,
			names = {"--includes", "-i"},
			converter = FilterConverter.class,
			description = "A regular expression pattern of types to include." +
					"You can specify multiple patterns by repeating the -i option. " +
					"Default: \".*\""
	)
	private List<Filter> includes;

	@Parameter(order = 3,
			names = {"--excludes", "-e"},
			converter = FilterConverter.class,
			description = "A regular expression pattern of types to exclude. " +
					"You can specify multiple patterns by repeating the -e option. " +
					"Default: \"java.*\""
	)
	private List<Filter> excludes;

	@Parameter(order = 4,
			names = {"--format", "-f"},
			converter = FormatConverter.class,
			description = "The output format. Supported formats are:" +
					"\n\tCYTOSCAPE_JS - html page using cytoscape.js to render the graph." +
					"\n\tDOT - a dot file that can be transformed to other formats using the dot tool." +
					"Default: CYTOSCAPE_JS"
	)
	private Format format;

	@Parameter(order = 10,
			names = {"--cycles-only"},
			description = "If specified only classes that are part of a cyclic dependency are shown."
	)
	private boolean cyclesOnly;

	@Parameter(order = 11,
			names = {"--show-self-references"},
			description = "If specified classes that only contain self references are shown. " +
					"By default these are omitted."
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
		switch (format) {
			case CYTOSCAPE_JS:
				return new CytoscapeReportTransformer();
			case DOT:
				return new DotReportTransformer();
		}
		throw new IllegalArgumentException("Transformer missing for format " + format);
	}

	private BufferedWriter getOutputWriter(String extension) throws IOException {
		if (outputDir == null) {
			return new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
		}

		Files.createDirectories(outputDir);
		final Path targetPath = outputDir.resolve("dependency-graph." + extension);
		return Files.newBufferedWriter(targetPath);
	}

	@Override
	public String getDefaultValueFor(String optionName) {
		switch (optionName) {
			case "--includes":
				return ".*";
			case "--excludes":
				return "java.*";
			case "--format":
				return Format.CYTOSCAPE_JS.name();
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

	private static class FormatConverter implements IStringConverter<Format> {
		@Override
		public Format convert(String value) {
			return Format.valueOf(value.toUpperCase());
		}
	}

	public enum Format {
		CYTOSCAPE_JS, DOT
	}
}
