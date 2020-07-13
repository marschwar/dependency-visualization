package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {
	Collection<Path> sources;
	Filters filters;

	public void generate() {
		final Parser parser = new Parser();
		final long start = System.currentTimeMillis();
		final List<Node> nodes = sources.stream()
				.map(parser::parse)
				.map(this::createTypeReport)
				.filter(filters::isIncluded)
				.map(this::filterReferencedTypes)
				.map(this::createNode)
				.collect(Collectors.toList());
		final long duration = System.currentTimeMillis() - start;
		for (Node node : nodes) {
			System.out.println(node);
		}

		System.out.println("Processing of " + sources.size() + " source files took " + duration + " ms.");
	}

	private TypeReport filterReferencedTypes(TypeReport unfiltered) {
		return unfiltered.toBuilder()
				.referencedTypes(filters.apply(unfiltered.getReferencedTypes()))
				.build();
	}

	private Node createNode(TypeReport report) {
		return Node.builder()
				.source(report.getQualifiedName())
				.targets(report.getReferencedTypes().stream()
						.map(ReferencedType::getQualifiedName)
						.sorted()
						.collect(Collectors.toList()))
				.build();
	}

	private TypeReport createTypeReport(ParseTree parseTree) {
		final ReferencedTypesListener listener = new ReferencedTypesListener();
		new ParseTreeWalker().walk(listener, parseTree);
		return listener.createReport();
	}
}
