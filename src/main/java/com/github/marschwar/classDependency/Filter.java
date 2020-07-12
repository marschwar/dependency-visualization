package com.github.marschwar.classDependency;

import java.util.regex.Pattern;

public class Filter {
	private final Pattern pattern;

	private Filter(Pattern pattern) {
		this.pattern = pattern;
	}

	public static Filter of(String pattern) {
		return new Filter(Pattern.compile(pattern));
	}

	public boolean matches(Filterable source) {
		return pattern.matcher(source.getQualifiedName()).matches();
	}
}
