package com.github.marschwar.depvis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdepsOutputParser {
	private static final Pattern JDEPS_PATTERN = Pattern.compile(
			"\\s*" +
					"(?<sourcePackage>[.\\w]+)\\.(?<sourceType>\\w+)(?:\\$\\w+)?" +
					"\\s*->\\s" +
					"(?<targetPackage>[.\\w]+)\\.(?<targetType>\\w+)(?:\\$\\w+)?" +
					"\\s*.*");

	public ClassDependency parse(String outputLine) {
		if (outputLine == null) {
			return null;
		}
		final Matcher matcher = JDEPS_PATTERN.matcher(outputLine);
		if (matcher.matches()) {
			return ClassDependency.builder()
					.source(ReferencedType.of(
							matcher.group("sourcePackage"),
							matcher.group("sourceType")))
					.target(ReferencedType.of(
							matcher.group("targetPackage"),
							matcher.group("targetType")))
					.build();
		}
		return null;
	}
}
