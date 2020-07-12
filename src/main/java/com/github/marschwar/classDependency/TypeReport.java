package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class TypeReport implements Filterable {
	PackageDeclaration packageDeclaration;
	String name;
	Set<ReferencedType> referencedTypes;

	@Override
	public String getQualifiedName() {
		return packageDeclaration.getPackageName() + "." + name;
	}
}
