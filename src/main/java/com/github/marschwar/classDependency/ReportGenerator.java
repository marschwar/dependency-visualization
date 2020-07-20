package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {

	Path sourcePath;
	Filters filters;
	Logger logger;

	public Report generate() throws ReportGenerationException {
		return extractDependenciesAndCreateReport();
	}

	private Report extractDependenciesAndCreateReport() throws ReportGenerationException {
		final ClassDependencyExtractor extractor = new ClassDependencyExtractor(logger);
		final Set<ClassDependency> dependencies = extractor.extractDependencies(sourcePath);

		final List<ClassDependency> filteredDependencies = dependencies.stream()
				.filter(classDependency ->
						filters.isIncluded(classDependency.getSource())
								&& filters.isIncluded(classDependency.getTarget())
				)
				.distinct()
				.sorted()
				.collect(Collectors.toList());

		return Report.builder().dependencies(filteredDependencies).build();

	}
}
