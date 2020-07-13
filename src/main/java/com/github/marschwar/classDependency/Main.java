package com.github.marschwar.classDependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class Main {

	public static void main(String[] args) {
		final Path sourceDir = Paths.get(args[0]);
		try {
			final List<Path> sourceFiles = Files.find(sourceDir, 10, filter())
					.collect(Collectors.toList());

			final ReportGenerator generator = ReportGenerator.builder()
					.filters(Filters.builder()
							.include("de\\.hermes\\.delta\\.service.*")
							.exclude("de\\.hermes\\.delta\\.service\\.ServiceFactory")
							.build())
					.sources(sourceFiles)
					.build();
			generator.generate();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static BiPredicate<Path, BasicFileAttributes> filter() {
		return (path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
				&& path.getFileName().toString().endsWith(".java");
	}
}
