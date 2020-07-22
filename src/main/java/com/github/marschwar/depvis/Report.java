package com.github.marschwar.depvis;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Report {
	List<ClassDependency> dependencies;
}
