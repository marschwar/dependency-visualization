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
				"bar.Other",
				"foo.A",
				"bar.B",
				"foo.C",
				"bar.D",
				"java.lang.Object"
		);
	}

	@Test
	void testFieldReferences() {
		Parsers.parse("/FieldReferences.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"bar.B",
				"foo.C",
				"foo.bar.D",
				"java.lang.String"
		);
	}

	@Test
	void testCreators() {
		Parsers.parse("/Creators.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"bar.Outer",
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

	@Test
	void testImportedTypes() {
		Parsers.parse("/ImportedTypes.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"bar.A",
				"java.lang.Runnable",
				"java.util.Arrays",
				"java.util.List",
				"java.lang.String"
		);
	}

	@Test
	void testFieldsAndVariables() {
		Parsers.parse("/FieldsAndVariables.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"foo.B",
				"bar.C",
				"java.lang.Integer",
				"java.lang.String"
		);
	}

	@Test
	void testParametrized() {
		Parsers.parse("/ParametrizedTypes.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"bar.B",
				"java.lang.Integer",
				"java.lang.String",
				"java.util.List",
				"java.util.Map"
		);
	}

	@Test
	void testEnums() {
		Parsers.parse("/Enums.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"java.lang.String"
		);
	}

	@Test
	void testIgnoreSelf() {
		Parsers.parse("/IgnoreSelf.java", underTest);

		assertThat(underTest.getTypes()).isEmpty();
	}

	@Test
	void testConstants() {
		Parsers.parse("/Constants.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"java.lang.String",
				"java.util.List",
				"java.util.Arrays"
		);
	}

	@Test
	void testIgnoreInner() {
		Parsers.parse("/IgnoreInner.java", underTest);

		assertThat(underTest.getTypes()).isEmpty();
	}
}