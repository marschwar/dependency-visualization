package com.github.marschwar.classDependency.cytoscape;

import com.github.marschwar.classDependency.Report;
import com.github.marschwar.classDependency.ReportTransformer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CytoscapeReportTransformer implements ReportTransformer {

	public static final String ELEMENTS_PLACEHOLDER = "/*ELEMENTS_PLACEHOLDER*/";
	public static final String TEMPLATE_RESOURCE = "/cytoscape/template.html";

	@Override
	public void transform(Report report, Writer writer) throws IOException {
		final Gson gson = new GsonBuilder()
				.setPrettyPrinting().create();

		try (StringWriter elementsWriter = new StringWriter()) {
			final JsonWriter jsonWriter = gson.newJsonWriter(elementsWriter);
			final Elements container = convert(report);
			gson.toJson(container, Elements.class, jsonWriter);

			final String elements = elementsWriter.toString();
			final String elementsReplacement = "\"elements\": " + elements;
			try (BufferedReader templateReader = openTemplateReader()) {
				String line;
				while ((line = templateReader.readLine()) != null) {
					writer.write(line.replace(ELEMENTS_PLACEHOLDER, elementsReplacement));
					writer.write("\n");
				}
			}
		}

	}

	private BufferedReader openTemplateReader() {
		final InputStream resourceAsStream = getClass().getResourceAsStream(TEMPLATE_RESOURCE);
		Objects.requireNonNull(resourceAsStream);
		return new BufferedReader(new InputStreamReader(resourceAsStream));
	}

	private Elements convert(Report report) {
		List<Node> nodes = new ArrayList<>();
		List<Edge> edges = new ArrayList<>();

		report.getTypes().forEach(typeReport -> {
			final String typeId = typeReport.getQualifiedName();
			nodes.add(Node.of(typeId, typeReport.getName()));
			edges.addAll(typeReport.getReferencedTypes().stream()
					.map(referencedType -> Edge.of(typeId, referencedType.getQualifiedName()))
					.collect(Collectors.toList()));
		});
		return Elements.builder()
				.nodes(nodes)
				.edges(edges)
				.build();
	}
}
