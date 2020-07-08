package com.github.marschwar.classDependency;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;

public class Parsers {

	public static void parse(String classPathResource, ParseTreeListener listener) {
		try (InputStream in = Parsers.class.getResourceAsStream(classPathResource)) {
			final ParseTree parseTree = new Parser().parse(CharStreams.fromStream(in));
			new ParseTreeWalker().walk(listener, parseTree);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
