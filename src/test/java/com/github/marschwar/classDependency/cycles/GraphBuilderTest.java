package com.github.marschwar.classDependency.cycles;


import com.github.marschwar.classDependency.cycles.Graph.GraphBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBuilderTest {

	private GraphBuilder builder = new GraphBuilder();

	@Test
	public void emptyGraph() {
		final Graph result = builder.build();

		assertThat(result.getVertices()).isEmpty();
	}

	@Test
	public void singleEdge() {
		final Graph result = builder
				.addEdge("a", "b")
				.build();

		assertThat(result.getVertices()).hasSize(2);
	}

	@Test
	public void sameEdgeTwice() {
		final Graph result = builder
				.addEdge("a", "b")
				.addEdge("a", "b")
				.build();

		assertThat(result.getVertices()).hasSize(2);
	}

	@Test
	public void multipleEdges() {
		final Graph result = builder
				.addEdge("a", "b")
				.addEdge("c", "d")
				.addEdge("a", "d")
				.build();

		assertThat(result.getVertices()).hasSize(4);
	}
}