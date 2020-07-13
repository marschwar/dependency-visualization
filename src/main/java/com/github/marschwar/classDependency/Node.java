package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Node {
	String source;
	List<String> targets;
}
