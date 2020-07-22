package com.github.marschwar.depvis;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferencedType implements Filterable, Comparable<ReferencedType> {
	String packageDeclaration;
	String name;

	public static ReferencedType of(String packageName, String typeName) {
		return ReferencedType.builder()
				.packageDeclaration(packageName)
				.name(typeName)
				.build();
	}

	public String getQualifiedName() {
		return packageDeclaration + "." + name;
	}

	@Override
	public int compareTo(ReferencedType other) {
		return getQualifiedName().compareTo(other.getQualifiedName());
	}

	@Override
	public String toString() {
		return getQualifiedName();
	}
}
