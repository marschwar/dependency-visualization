package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Report {
	List<TypeReport> types;
}
