package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.cytoscape.CytoscapeReportTransformer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;

public class Main {

	public static void main(String[] args) {
		final Path sourcePath = Paths.get(args[0]);

		final ReportGenerator generator = ReportGenerator.builder()
				.filters(Filters.builder()
						.include("de\\.hermes\\.delta\\.service.*")
						.exclude("de\\.hermes\\.delta\\.service\\.ServiceFactory")
						.exclude("de\\.hermes\\.delta\\.service\\.LogService")
						.build())
				.sourcePath(sourcePath)
				.build();
		final Report report = generator.generate();

		try (Writer writer = new BufferedWriter(new FileWriter("/tmp/deps.html"))) {
			new CytoscapeReportTransformer().transform(report, writer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static BiPredicate<Path, BasicFileAttributes> filter() {
		return (path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
				&& path.getFileName().toString().endsWith(".java");
	}
}
