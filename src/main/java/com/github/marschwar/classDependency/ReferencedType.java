package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferencedType implements Filterable, Comparable<ReferencedType> {
	PackageDeclaration packageDeclaration;
	String name;

	public static ReferencedType of(PackageDeclaration packageName, String typeName) {
		return ReferencedType.builder()
				.packageDeclaration(packageName)
				.name(typeName)
				.build();
	}

	public static ReferencedType of(String packageName, String typeName) {
		return ReferencedType.of(new PackageDeclaration(packageName), typeName);
	}

	public String getQualifiedName() {
		return packageDeclaration.getPackageName() + "." + name;
	}

	@Override
	public int compareTo(ReferencedType other) {
		return getQualifiedName().compareTo(other.getQualifiedName());
	}
}
