package com.github.marschwar.depvis.cycles;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VertexTest {

	@Test
	public void sameEdge() {
		final Vertex source = new Vertex("a");
		final Vertex target = new Vertex("b");

		source.addEdge(target);
		source.addEdge(target);

		assertThat(source.getEdges()).hasSize(1);
	}
}