package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.parser.JavaLexer;
import com.github.marschwar.classDependency.parser.JavaParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Parser {

	public ParseTree parse(CharStream input) {
		final JavaLexer lexer = new JavaLexer(input);
		final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		final JavaParser javaParser = new JavaParser(tokenStream);
		return javaParser.compilationUnit();
	}
}
