package com.github.marschwar.depvis.cytoscape;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NodeData {
	String id;
	String name;
}
