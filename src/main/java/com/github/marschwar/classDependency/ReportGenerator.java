package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
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
			return runJdepAndCreateReport();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Report runJdepAndCreateReport() throws IOException {
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

		final List<TypeReport> typeReports = new ArrayList<>();
		final Map<ReferencedType, List<ClassDependency>> depMap = dependencies.stream().collect(Collectors.groupingBy(ClassDependency::getSource));
		for (Map.Entry<ReferencedType, List<ClassDependency>> entry : depMap.entrySet()) {
			final ReferencedType source = entry.getKey();
			if (!filters.isIncluded(source)) {
				continue;
			}
			final List<ReferencedType> targets = entry.getValue().stream()
					.map(ClassDependency::getTarget)
					.filter(filters::isIncluded)
					.distinct()
					.sorted()
					.collect(Collectors.toList());

			typeReports.add(
					TypeReport.builder()
							.packageDeclaration(source.getPackageDeclaration())
							.name(source.getName())
							.referencedTypes(targets)
							.build());
		}

		try {
			final int exitCode = process.waitFor();
			System.out.println("jdeps finished with exitCode " + exitCode);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return Report.builder().types(typeReports).build();

	}
}
