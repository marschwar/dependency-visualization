package com.github.marschwar.classDependency.cycles;

import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
public class Vertex {
	private final String id;
	private boolean visited = false;
	private final Set<Vertex> edges = new HashSet<>();

	public void addEdge(Vertex vertex) {
		edges.add(vertex);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vertex vertex = (Vertex) o;
		return Objects.equals(id, vertex.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

}
