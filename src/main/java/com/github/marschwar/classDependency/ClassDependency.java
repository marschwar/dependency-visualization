package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClassDependency {
	ReferencedType source;
	ReferencedType target;
}
