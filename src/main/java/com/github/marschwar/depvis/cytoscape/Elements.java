package com.github.marschwar.depvis.cytoscape;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Elements {
	List<Node> nodes;
	List<Edge> edges;
}
