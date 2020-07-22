package com.github.marschwar.depvis;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.github.marschwar.depvis.SilentLogger.SILENT_LOGGER;
import static org.assertj.core.api.Assertions.assertThat;

class ClassDependencyExtractorTest {

	@Test
	public void extractOwnDependencies() throws Exception {
		final Path sourcePath = getPathOfClassFile();

		final Set<ClassDependency> dependencies = new ClassDependencyExtractor(SILENT_LOGGER).extractDependencies(sourcePath);
		assertThat(dependencies)
				.extracting(ClassDependency::getTarget)
				.contains(
						ReferencedType.of("com.github.marschwar.classDependency", "Logger"),
						ReferencedType.of("java.nio.file", "Path")
				);
	}

	private Path getPathOfClassFile() throws IOException {
		final String classFileName = ClassDependencyExtractorTest.class.getSimpleName() + ".class";
		final BiPredicate<Path, BasicFileAttributes> predicate = ((path, basicFileAttributes) ->
				basicFileAttributes.isRegularFile() && path.endsWith(classFileName));
		return Files.find(Paths.get("."), Integer.MAX_VALUE, predicate).findFirst().get();
	}
}