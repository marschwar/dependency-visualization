package com.github.marschwar.classDependency.cytoscape;

import lombok.Value;

@Value
public class Node {
	NodeData data;

	public static Node of(String id, String name) {
		return new Node(
				NodeData.builder()
						.id(id)
						.name(name)
						.build()
		);
	}
}
