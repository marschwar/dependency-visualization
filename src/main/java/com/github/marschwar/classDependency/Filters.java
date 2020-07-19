package com.github.marschwar.classDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Filters {
	private final List<Filter> includes;
	private final List<Filter> excludes;

	public Filters(List<Filter> includes, List<Filter> excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}


	public <T extends Filterable> Set<T> apply(Collection<T> source) {
		return source.stream()
				.filter(this::isIncluded)
				.collect(Collectors.toSet());
	}

	public boolean isIncluded(Filterable item) {
		if (!includes.isEmpty() && includes.stream().noneMatch(filter -> filter.matches(item))) {
			return false;
		}
		return excludes.stream().noneMatch(filter -> filter.matches(item));
	}

	@Override
	public String toString() {
		return "includes=" + includes + ", excludes=" + excludes;
	}

	public static FiltersBuilder builder() {
		return new FiltersBuilder();
	}

	public static class FiltersBuilder {
		private final List<Filter> includes = new ArrayList<>();
		private final List<Filter> excludes = new ArrayList<>();

		public FiltersBuilder include(String pattern) {
			includes.add(Filter.of(pattern));
			return this;
		}

		public FiltersBuilder exclude(String pattern) {
			excludes.add(Filter.of(pattern));
			return this;
		}

		public Filters build() {
			return new Filters(includes, excludes);
		}
	}
}
