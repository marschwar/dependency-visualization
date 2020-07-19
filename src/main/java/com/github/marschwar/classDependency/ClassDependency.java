package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClassDependency implements Comparable<ClassDependency> {
	ReferencedType source;
	ReferencedType target;

	@Override
	public int compareTo(ClassDependency other) {
		final int bySource = source.compareTo(other.source);
		return bySource == 0 ? target.compareTo(other.target) : bySource;
	}
}
