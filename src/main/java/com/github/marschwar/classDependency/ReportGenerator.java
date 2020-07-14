package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {

	Path sourcePath;
	Filters filters;

	public Report generate() {
		try {
			return analyze();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private TypeReport filterReferencedTypes(TypeReport unfiltered) {
		final ArrayList<ReferencedType> filteredTypes = new ArrayList<>(filters.apply(unfiltered.getReferencedTypes()));
		Collections.sort(filteredTypes);
		return unfiltered.toBuilder()
				.referencedTypes(filteredTypes)
				.build();
	}

	private TypeReport createTypeReport(ParseTree parseTree) {
		final ReferencedTypesListener listener = new ReferencedTypesListener();
		new ParseTreeWalker().walk(listener, parseTree);
		return listener.createReport();
	}

	private Report analyze() throws IOException {
		final Process process = Runtime.getRuntime().exec(new String[]{
				"jdeps",
				"-verbose",
				sourcePath.toAbsolutePath().toString()
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

		final int exitCode;
		try {
			exitCode = process.waitFor();
			System.out.println("jdeps finished with exitCode " + exitCode);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		final List<TypeReport> typeReports = new ArrayList<>();
		final Map<ReferencedType, List<ClassDependency>> depMap = dependencies.stream().collect(Collectors.groupingBy(ClassDependency::getSource));
		for (Map.Entry<ReferencedType, List<ClassDependency>> entry : depMap.entrySet()) {
			final ReferencedType source = entry.getKey();
			if (!filters.isIncluded(source)) {
				continue;
			}
			final Set<ReferencedType> uniqueTargets = entry.getValue().stream()
					.map(ClassDependency::getTarget)
					.filter(filters::isIncluded)
					.collect(Collectors.toSet());
			final List<ReferencedType> targets = new ArrayList<>(uniqueTargets);
			Collections.sort(targets);
			typeReports.add(
					TypeReport.builder()
							.packageDeclaration(source.getPackageDeclaration())
							.name(source.getName())
							.referencedTypes(targets)
							.build());
		}

		return Report.builder().types(typeReports).build();

	}
}
