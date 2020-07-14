package com.github.marschwar.classDependency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdepsOutputParserTest {

	private final JdepsOutputParser underTest = new JdepsOutputParser();

	@Test
	void parsesNull() {
		final String line = null;
		final ClassDependency result = underTest.parse(line);
		assertThat(result).isNull();
	}

	@Test
	void parsesUnknown() {
		final String line = "any unknown string";
		final ClassDependency result = underTest.parse(line);
		assertThat(result).isNull();
	}

	@Test
	void parsesJavaLangDependency() {
		final String line = "   foo.Service  -> java.lang.String";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("java.lang", "String"))
				.build();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void parsesDependencyUnknownOrigin() {
		final String line = "   foo.Service          -> bar.Bar (not found)";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("bar", "Bar"))
				.build();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void parsesDependencyKnownOrigin() {
		final String line = "   foo.Service          -> bar.Bar           bar";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("bar", "Bar"))
				.build();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void parsesDependencySourceInnerClass() {
		final String line = "   foo.Service$Inner          -> bar.Bar (bar)";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("bar", "Bar"))
				.build();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void parsesDependencyTargetInnerClass() {
		final String line = "   foo.Service          -> bar.Bar$Inner";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("bar", "Bar"))
				.build();
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void parsesDependencySourceAndTargetInnerClass() {
		final String line = "   foo.Service$Inner          -> bar.Bar$Inner";
		final ClassDependency result = underTest.parse(line);
		final ClassDependency expected = ClassDependency.builder()
				.source(ReferencedType.of("foo", "Service"))
				.target(ReferencedType.of("bar", "Bar"))
				.build();
		assertThat(result).isEqualTo(expected);
	}
}