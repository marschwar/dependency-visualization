package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.parser.JavaParser;
import com.github.marschwar.classDependency.parser.JavaParserBaseListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReferencedTypesListener extends JavaParserBaseListener {

	private String packageName;

	private final Set<String> types = new HashSet<>();

	@Override
	public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
		packageName = ctx.qualifiedName().getText();
	}

	@Override
	public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
		String type = ctx.qualifiedName().getText();
		if (ctx.STATIC() != null && ctx.MUL() == null) {
			// remove method name in static method import
			type = type.substring(0, type.lastIndexOf("."));
		}
		types.add(type);
	}

	public Set<String> getTypes() {
		return Collections.unmodifiableSet(types);
	}
}
