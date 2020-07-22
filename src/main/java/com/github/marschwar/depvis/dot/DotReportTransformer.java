package com.github.marschwar.depvis.dot;

import com.github.marschwar.depvis.ClassDependency;
import com.github.marschwar.depvis.ReferencedType;
import com.github.marschwar.depvis.Report;
import com.github.marschwar.depvis.ReportTransformer;

import java.io.IOException;
import java.io.Writer;

public class DotReportTransformer implements ReportTransformer {
	@Override
	public void transform(Report report, Writer writer) throws IOException {
		writer.write("digraph dependency_graph\n");
		writer.write("{\n");
		for (ReferencedType node : report.getNodes()) {
			writer.write(
					String.format("\t\"%s\" [label=%s];\n",
							node.getQualifiedName(),
							node.getName())
			);
		}
		writer.write("\n");
		for (ClassDependency edge : report.getEdges()) {
			writer.write(
					String.format("\t\"%s\" -> \"%s\";\n",
							edge.getSource().getQualifiedName(),
							edge.getTarget().getQualifiedName())
			);
		}
		writer.write("}\n");

	}

	@Override
	public String getFileExtension() {
		return "dot";
	}
}
