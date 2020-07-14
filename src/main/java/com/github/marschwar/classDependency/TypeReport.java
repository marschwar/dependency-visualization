package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class TypeReport implements Filterable, Comparable<TypeReport> {
	PackageDeclaration packageDeclaration;
	String name;
	List<ReferencedType> referencedTypes;

	@Override
	public String getQualifiedName() {
		return packageDeclaration.getPackageName() + "." + name;
	}

	@Override
	public int compareTo(TypeReport other) {
		return getQualifiedName().compareTo(other.getQualifiedName());
	}
}
