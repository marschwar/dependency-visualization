package com.github.marschwar.depvis;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
@Builder
public class Report {
	String name;
	List<ClassDependency> edges;

	public List<ReferencedType> getNodes() {
		return edges.stream()
				.flatMap(it -> Stream.of(it.getSource(), it.getTarget()))
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}
}
