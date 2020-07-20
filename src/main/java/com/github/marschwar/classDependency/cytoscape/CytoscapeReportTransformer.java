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
		List<Node> nodes = new ArrayList<>();
		List<Edge> edges = new ArrayList<>();

		report.getDependencies().forEach(dep -> {
			nodes.add(Node.of(dep.getSource().getQualifiedName(), dep.getSource().getName()));
			nodes.add(Node.of(dep.getTarget().getQualifiedName(), dep.getTarget().getName()));
			edges.add(Edge.of(dep.getSource().getQualifiedName(), dep.getTarget().getQualifiedName()));
		});

		return Elements.builder()
				.nodes(nodes.stream().distinct().collect(Collectors.toList()))
				.edges(edges)
				.build();
	}
}
