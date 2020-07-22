package com.github.marschwar.depvis.cytoscape;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EdgeData {
	String source;
	String target;
}
