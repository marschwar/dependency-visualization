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
		Parsers.parse("/ImportTestClass.java", underTest);

		assertThat(underTest.getTypes()).containsExactlyInAnyOrder(
				"foo.A",
				"foo.B",
				"foo.C"
		);
	}
}