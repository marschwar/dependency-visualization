package com.github.marschwar.classDependency;

import lombok.Builder;
import lombok.Value;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
public class ReportGenerator {

	Collection<Path> sources;
	Filters filters;

	public Report generate() {
		final Parser parser = new Parser();
		final long start = System.currentTimeMillis();
		final List<TypeReport> typeReports = sources.stream()
				.map(parser::parse)
				.map(this::createTypeReport)
				.filter(filters::isIncluded)
				.map(this::filterReferencedTypes)
				.sorted()
				.collect(Collectors.toList());

		final long duration = System.currentTimeMillis() - start;
		System.out.println("Processing of " + sources.size() + " source files took " + duration + " ms.");

		return Report.builder().types(typeReports).build();
	}

	private TypeReport filterReferencedTypes(TypeReport unfiltered) {
		final ArrayList<ReferencedType> filteredTypes = new ArrayList<>(filters.apply(unfiltered.getReferencedTypes()));
		Collections.sort(filteredTypes);
		return unfiltered.toBuilder()
				.referencedTypes(filteredTypes)
				.build();
	}

	private TypeReport createTypeReport(ParseTree parseTree) {
		final ReferencedTypesListener listener = new ReferencedTypesListener();
		new ParseTreeWalker().walk(listener, parseTree);
		return listener.createReport();
	}
}
