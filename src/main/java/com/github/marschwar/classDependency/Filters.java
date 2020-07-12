package com.github.marschwar.classDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Filters {
	private final List<Filter> includes;
	private final List<Filter> excludes;

	public Filters(List<Filter> includes, List<Filter> excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}


	public <T extends Filterable> Collection<T> apply(Collection<T> source) {
		return source.stream()
				.filter(item -> includes.isEmpty() || includes.stream().anyMatch(filter -> filter.matches(item)))
				.filter(item -> excludes.stream().noneMatch(filter -> filter.matches(item)))
				.collect(Collectors.toSet());
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
