package com.github.marschwar.classDependency;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilterTest {

	@Test
	public void testMatchAll() {
		final Filter filter = Filter.of(".*");

		final TestFilterable filterable = new TestFilterable("a.b.C");

		assertThat(filter.matches(filterable)).isTrue();
	}

	@Test
	public void testMatchOther() {
		final Filter filter = Filter.of("x.");

		final TestFilterable filterable = new TestFilterable("a.b.C");

		assertThat(filter.matches(filterable)).isFalse();
	}

}