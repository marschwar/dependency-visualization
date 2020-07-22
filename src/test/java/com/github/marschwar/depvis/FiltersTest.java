package com.github.marschwar.depvis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FiltersTest {

	@DisplayName("include all if no filters specified")
	@Test
	public void noFilters() {
		final Filters filters = Filters.builder().build();
		final Set<TestFilterable> source = Collections.singleton(new TestFilterable("a.b.C"));

		final Collection<TestFilterable> result = filters.apply(source);

		assertThat(result).hasSameElementsAs(source);
	}

	@Test
	public void includesAndExcludes() {
		final Filters filters = Filters.builder()
				.include("a\\..*")
				.include("b\\..*")
				.exclude(".*\\.X")
				.build();
		final List<TestFilterable> source = Arrays.asList(
				new TestFilterable("a.b.C"),
				new TestFilterable("a.A"),
				new TestFilterable("b.B"),
				new TestFilterable("x.b.B"),
				new TestFilterable("a.b.X")
		);

		final Set<TestFilterable> result = filters.apply(source);

		assertThat(result)
				.extracting(TestFilterable::getQualifiedName)
				.containsExactlyInAnyOrder(
						"a.b.C",
						"a.A",
						"b.B"
				);
	}

}