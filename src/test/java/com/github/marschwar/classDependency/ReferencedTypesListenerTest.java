package com.github.marschwar.classDependency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ReferencedTypesListenerTest {

	private ReferencedTypesListener underTest;

	@BeforeEach
	public void createListener() {
		underTest = new ReferencedTypesListener();
	}

	@Test
	void testImports() {
		Parsers.parse("/Imports.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"foo.B",
				"foo.C"
		);
	}

	@Test
	void testMethodReferences() {
		Parsers.parse("/MethodReferences.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"bar.B",
				"foo.C",
				"bar.D"
		);
	}

	@Test
	void testFieldReferences() {
		Parsers.parse("/FieldReferences.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"bar.B",
				"foo.C",
				"foo.bar.D"
		);
	}

	@Test
	void testCreators() {
		Parsers.parse("/Creators.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"java.util.function.Supplier",
				"java.lang.String",
				"foo.A",
				"bar.B",
				"foo.C",
				"foo.D",
				"bar.E"
		);
	}

	@Test
	void testJavaLangTypes() {
		Parsers.parse("/JavaLang.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"java.lang.Object",
				"java.lang.String"
		);
	}
}