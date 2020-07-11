package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.parser.JavaParser;
import com.github.marschwar.classDependency.parser.JavaParser.CreatorContext;
import com.github.marschwar.classDependency.parser.JavaParser.ExpressionContext;
import com.github.marschwar.classDependency.parser.JavaParser.ImportDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.MethodCallContext;
import com.github.marschwar.classDependency.parser.JavaParser.PackageDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParserBaseListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class ReferencedTypesListener extends JavaParserBaseListener {

	private String packageName;

	private final Set<String> types = new HashSet<>();
	private final Stack<Set<String>> variables = new Stack<>();

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		packageName = ctx.qualifiedName().getText();
	}

	@Override
	public void enterClassBody(JavaParser.ClassBodyContext ctx) {
		variables.push(new HashSet<>());
	}

	@Override
	public void enterBlock(JavaParser.BlockContext ctx) {
		variables.push(new HashSet<>());
	}

	@Override
	public void exitBlock(JavaParser.BlockContext ctx) {
		variables.pop();
	}

	@Override
	public void enterVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
		variables.peek().add(ctx.IDENTIFIER().getText());
	}

	@Override
	public void enterImportDeclaration(ImportDeclarationContext ctx) {
		String type = ctx.qualifiedName().getText();
		if (ctx.STATIC() != null && ctx.MUL() == null) {
			// remove method name in static method import
			type = type.substring(0, type.lastIndexOf("."));
		}
		types.add(type);
	}

	@Override
	public void enterCreator(CreatorContext ctx) {
		final String createdName = ctx.createdName().getText();

		types.add(createdName);
	}

	@Override
	public void enterMethodCall(MethodCallContext ctx) {
		final ExpressionContext parent = (ExpressionContext) ctx.getParent();
		final List<ExpressionContext> expressions = parent.expression();
		if (expressions.size() != 1) {
			return;
		}

		final ExpressionContext expression = expressions.get(0);
		if (expression.creator() != null) {
			return;
		}
		if (expression.methodCall() != null) {
			return;
		}
		if (expression.primary() != null && expression.primary().THIS() != null) {
			return;
		}

		String receiver = expression.getText();

		if (isVariable(receiver)) {
			return;
		}
		if (expression.getChildCount() == 1) {
			receiver = packageName + "." + receiver;
		}
		types.add(receiver);
	}

	private boolean isVariable(String name) {
		return variables.stream().anyMatch(variablesInBlock -> variablesInBlock.contains(name));
	}

	public Set<String> getTypes() {
		return Collections.unmodifiableSet(types);
	}
}
