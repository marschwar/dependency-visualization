package com.github.marschwar.depvis;

import com.github.marschwar.depvis.cycles.Graph.GraphBuilder;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {

	Path sourcePath;
	Logger logger;
	Filters filters;
	boolean cyclesOnly;
	boolean showSelfReferences;

	public Report generate() throws ReportGenerationException {
		return extractDependenciesAndCreateReport();
	}

	private Report extractDependenciesAndCreateReport() throws ReportGenerationException {
		final ClassDependencyExtractor extractor = new ClassDependencyExtractor(logger);
		final Set<ClassDependency> dependencies = extractor.extractDependencies(sourcePath);

		final Set<ClassDependency> filteredDependencies = dependencies.stream()
				.filter(it ->
						filters.isIncluded(it.getSource())
								&& filters.isIncluded(it.getTarget())
				)
				.filter(it -> showSelfReferences
						|| !it.getSource().equals(it.getTarget()))
				.collect(Collectors.toSet());

		final Predicate<ClassDependency> cyclesPredicate = createCyclesPredicate(filteredDependencies);

		final List<ClassDependency> reportDependencies = filteredDependencies.stream()
				.filter(cyclesPredicate)
				.distinct()
				.sorted()
				.collect(Collectors.toList());

		return Report.builder()
				.edges(reportDependencies)
				.build();

	}

	private Predicate<ClassDependency> createCyclesPredicate(Set<ClassDependency> dependencies) {
		if (!cyclesOnly) {
			return (d) -> true;
		}
		final GraphBuilder builder = new GraphBuilder();
		dependencies.forEach(it ->
				builder.addEdge(
						it.getSource().getQualifiedName(),
						it.getTarget().getQualifiedName()
				)
		);
		final Set<String> cycles = builder.build().detectCycles();


		return (dep) -> cycles.contains(dep.getSource().getQualifiedName())
				&& cycles.contains(dep.getTarget().getQualifiedName());
	}
}
