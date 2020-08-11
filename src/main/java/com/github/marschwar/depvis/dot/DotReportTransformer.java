package com.github.marschwar.depvis.dot;

import com.github.marschwar.depvis.ClassDependency;
import com.github.marschwar.depvis.ReferencedType;
import com.github.marschwar.depvis.Report;
import com.github.marschwar.depvis.ReportTransformer;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class DotReportTransformer implements ReportTransformer {
	private static final List<String> KEYWORDS = Arrays.asList("node", "edge", "graph", "digraph", "subgraph", "strict");

	@Override
	public void transform(Report report, Writer writer) throws IOException {

		writer.write("digraph dependency_graph {\n");
		writer.write("\n");
		writer.write("\tnode [shape=box];\n");
		writer.write("\n");
		for (ReferencedType node : report.getNodes()) {

			// TODO: use subgraphs with different color per package
			/*
				subgraph {
				node [color=blue];
				"com.github.marschwar.depvis.dot.DotReportTransformer" [label=DotReportTransformer];
				}
			 */

			writer.write(
					String.format("\t\"%s\" [label=%s];%n",
							node.getQualifiedName(),
							getLabel(node))
			);
		}
		writer.write("\n");
		for (ClassDependency edge : report.getEdges()) {
			writer.write(
					String.format("\t\"%s\" -> \"%s\";%n",
							edge.getSource().getQualifiedName(),
							edge.getTarget().getQualifiedName())
			);
		}
		writer.write("}\n");

	}

	private String getLabel(ReferencedType node) {
		final String name = node.getName();
		if (KEYWORDS.contains(name.toLowerCase())) {
			return name + "_";
		}
		return name;
	}

	@Override
	public String getFileExtension() {
		return "dot";
	}
}
