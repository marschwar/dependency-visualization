package com.github.marschwar.classDependency.cycles;

import com.github.marschwar.classDependency.cycles.Graph.GraphBuilder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GraphTest {

	@Test
	public void cycles_emptyGraph() {
		final Graph graph = new GraphBuilder().build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).isEmpty();
	}

	@Test
	public void cycles_noCycle() {
		final Graph graph = new GraphBuilder()
				.addEdge("a", "b")
				.addEdge("a", "c")
				.addEdge("b", "c")
				.addEdge("c", "d")
				.addEdge("d", "e")
				.addEdge("b", "e")
				.build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).isEmpty();
	}

	@Test
	public void cycles_simpleCycle() {
		final Graph graph = new GraphBuilder()
				.addEdge("a", "b")
				.addEdge("b", "a")
				.build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).containsOnly("a", "b");
	}

	@Test
	public void cycles_cycleInTheEnd() {
		final Graph graph = new GraphBuilder()
				.addEdge("x", "a")
				.addEdge("a", "b")
				.addEdge("b", "c")
				.addEdge("c", "a")
				.build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).containsOnly("a", "b", "c");
	}

	@Test
	public void multipleCycles() {
		final Graph graph = new GraphBuilder()
				// first cycle
				.addEdge("a", "b")
				.addEdge("b", "c")
				.addEdge("c", "a")

				.addEdge("c", "d")
				.addEdge("d", "e")

				// second cycle
				.addEdge("e", "f")
				.addEdge("f", "e")
				.build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).containsOnly("a", "b", "c", "e", "f");
	}

	@Test
	public void ignoreSelfReference() {
		final Graph graph = new GraphBuilder()
				// first cycle
				.addEdge("a", "b")
				.addEdge("b", "c")
				.addEdge("c", "a")

				.addEdge("c", "d")
				.addEdge("d", "e")

				// self reference
				.addEdge("d", "d")

				.build();

		final Set<String> result = graph.detectCycles();

		assertThat(result).containsOnly("a", "b", "c");
	}
}