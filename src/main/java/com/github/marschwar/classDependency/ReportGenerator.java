package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {

	Path sourcePath;
	Filters filters;
	Logger logger;

	public Report generate() {
		try {
			return runJdepAndCreateReport();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Report runJdepAndCreateReport() throws IOException {
		final Path absolutePath = sourcePath.toAbsolutePath();
		logger.info("generating report for " + absolutePath + " using filters " + filters);
		final Process process = Runtime.getRuntime().exec(new String[]{
				"jdeps",
				"-verbose",
				absolutePath.toString()
		});
		final JdepsOutputParser parser = new JdepsOutputParser();
		final Set<ClassDependency> dependencies = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final ClassDependency dependencyOrNull = parser.parse(line);
				if (dependencyOrNull != null) {
					dependencies.add(dependencyOrNull);
				}
			}
		}

		final List<ClassDependency> filteredDepencencies = dependencies.stream()
				.filter(classDependency ->
						filters.isIncluded(classDependency.getSource())
								&& filters.isIncluded(classDependency.getTarget())
				)
				.distinct()
				.sorted()
				.collect(Collectors.toList());

		try {
			final int exitCode = process.waitFor();
			logger.info("jdeps finished with exitCode " + exitCode);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return Report.builder().dependencies(filteredDepencencies).build();

	}
}
