package com.github.marschwar.depvis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ClassDependencyExtractor {
	private final Logger logger;

	public ClassDependencyExtractor(Logger logger) {
		this.logger = logger;
	}

	public Set<ClassDependency> extractDependencies(Path sourcePath) throws ReportGenerationException {
		final Path absolutePath = sourcePath.toAbsolutePath();
		final String[] args = {
				"jdeps",
				"-verbose",
				absolutePath.toString()
		};
		final Process process;
		try {
			process = Runtime.getRuntime().exec(args);
		} catch (IOException e) {
			throw new ReportGenerationException("Error running jdeps", e);
		}

		final Set<ClassDependency> dependencies = parseOutput(process);

		try {
			final int exitCode = process.waitFor();
			logger.info("jdeps exited with code " + exitCode);
		} catch (InterruptedException e) {
			throw new ReportGenerationException("Error running jdeps", e);
		}
		return dependencies;
	}

	private Set<ClassDependency> parseOutput(Process process) throws ReportGenerationException {
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
		} catch (IOException e) {
			throw new ReportGenerationException("Unable to obtain output from jdeps", e);
		}

		return dependencies;
	}
}
