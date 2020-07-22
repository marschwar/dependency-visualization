package com.github.marschwar.depvis.cytoscape;

import com.cedarsoftware.util.io.JsonWriter;
import com.github.marschwar.depvis.Report;
import com.github.marschwar.depvis.ReportTransformer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CytoscapeReportTransformer implements ReportTransformer {

	public static final String ELEMENTS_PLACEHOLDER = "/*ELEMENTS_PLACEHOLDER*/";
	public static final String TEMPLATE_RESOURCE = "/cytoscape/template.html";

	@Override
	public void transform(Report report, Writer writer) throws IOException {


		final Elements container = convert(report);
		Map<String, Object> args = new HashMap<>();
		args.put("TYPE", false);
		args.put("PRETTY_PRINT", true);
		final String elements = JsonWriter.objectToJson(container, args);

		final String elementsReplacement = "\"elements\": " + elements;
		try (BufferedReader templateReader = openTemplateReader()) {
			String line;
			while ((line = templateReader.readLine()) != null) {
				writer.write(line.replace(ELEMENTS_PLACEHOLDER, elementsReplacement));
				writer.write("\n");
			}
		}

	}

	@Override
	public String getFileExtension() {
		return "html";
	}

	private BufferedReader openTemplateReader() {
		final InputStream resourceAsStream = getClass().getResourceAsStream(TEMPLATE_RESOURCE);
		Objects.requireNonNull(resourceAsStream);
		return new BufferedReader(new InputStreamReader(resourceAsStream));
	}

	private Elements convert(Report report) {
		List<Node> nodes = report.getNodes().stream()
				.map(it -> Node.of(it.getQualifiedName(), it.getName()))
				.collect(Collectors.toList());
		List<Edge> edges = report.getEdges().stream()
				.map(it -> Edge.of(it.getSource().getQualifiedName(), it.getTarget().getQualifiedName()))
				.collect(Collectors.toList());

		return Elements.builder()
				.nodes(nodes.stream().distinct().collect(Collectors.toList()))
				.edges(edges)
				.build();
	}
}
