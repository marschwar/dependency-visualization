package com.github.marschwar.depvis.cytoscape;


import lombok.Value;

@Value
public class Edge {
	EdgeData data;

	public static Edge of(String source, String target) {
		return new Edge(
				EdgeData.builder()
						.source(source)
						.target(target)
						.build()
		);
	}


}
