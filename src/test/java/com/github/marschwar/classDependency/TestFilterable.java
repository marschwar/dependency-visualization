package com.github.marschwar.classDependency;

import java.util.Objects;

public class TestFilterable implements Filterable {
	private final String qualifiedName;

	public TestFilterable(String qualifiedName) {
		this.qualifiedName = qualifiedName;
	}

	@Override
	public String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public String toString() {
		return qualifiedName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TestFilterable that = (TestFilterable) o;
		return Objects.equals(qualifiedName, that.qualifiedName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName);
	}
}
