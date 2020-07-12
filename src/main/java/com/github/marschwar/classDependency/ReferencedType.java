package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.util.function.Supplier;

@Value
@Builder
public class ReferencedType {
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

	public String toQualifiedName() {
		return packageDeclaration.getPackageName() + "." + name;
	}
}
