package com.github.marschwar.depvis.cycles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class Graph {
	private final Map<String, Vertex> vertices;


	private Graph(Map<String, Vertex> vertices) {
		this.vertices = vertices;
	}

	public Collection<Vertex> getVertices() {
		return Collections.unmodifiableCollection(vertices.values());
	}

	public Set<String> detectCycles() {
		final HashSet<String> cycles = new HashSet<>();
		final Collection<Vertex> verticesToVisit = this.vertices.values();
		verticesToVisit.forEach(vertex -> vertex.setVisited(false));
		verticesToVisit.forEach(vertex -> {
			cycles.addAll(detectCycles(vertex, emptyList()));
		});
		return cycles;
	}

	private Collection<String> detectCycles(Vertex vertex, List<String> path) {
		if (vertex.isVisited()) {
			return emptySet();
		}

		final int index = path.indexOf(vertex.getId());
		if (index > -1) {
			if (index == path.size() - 1) {
				// self reference
				return emptySet();
			}
			return path.subList(index, path.size());
		}

		final Set<String> cycles = new HashSet<>();
		vertex.getEdges().forEach(edge -> {
			final ArrayList<String> newPath = new ArrayList<>(path);
			newPath.add(vertex.getId());
			cycles.addAll(detectCycles(edge, newPath));
		});
		vertex.setVisited(true);
		return cycles;
	}

	@Override
	public String toString() {
		return vertices.values().toString();
	}

	public static class GraphBuilder {
		private final Map<String, Vertex> vertices = new HashMap<>();

		public GraphBuilder addEdge(String source, String target) {
			final Vertex sourceVertex = getOrCreateVertex(source);
			final Vertex targetVertex = getOrCreateVertex(target);
			sourceVertex.addEdge(targetVertex);
			return this;
		}

		private Vertex getOrCreateVertex(String id) {
			Vertex vertex = vertices.get(id);
			if (vertex == null) {
				vertex = new Vertex(id);
				vertices.put(id, vertex);
			}
			return vertex;
		}


		public Graph build() {
			return new Graph(vertices);
		}
	}
}
