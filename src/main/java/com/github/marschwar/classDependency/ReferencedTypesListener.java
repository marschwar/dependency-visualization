package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.parser.JavaParser;
import com.github.marschwar.classDependency.parser.JavaParser.CreatedNameContext;
import com.github.marschwar.classDependency.parser.JavaParser.ExpressionContext;
import com.github.marschwar.classDependency.parser.JavaParser.ImportDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.MethodCallContext;
import com.github.marschwar.classDependency.parser.JavaParser.PackageDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParserBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ReferencedTypesListener extends JavaParserBaseListener {

	public static final String DOT = ".";
	private PackageDeclaration packageName;

	private final Set<ReferencedType> types = new HashSet<>();
	private final Stack<Set<String>> variables = new Stack<>();

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		packageName = new PackageDeclaration(ctx.qualifiedName().getText());
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
		List<TerminalNode> nodes = ctx.qualifiedName().IDENTIFIER();
		if (ctx.STATIC() != null && ctx.MUL() == null) {
			// remove method name in static method import
			nodes = nodes.subList(0, nodes.size() - 1);
		}
		types.add(toReferencedType(nodes));
	}

	@Override
	public void enterCreatedName(CreatedNameContext ctx) {
		types.add(toReferencedType(ctx.IDENTIFIER()));
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
			types.add(ReferencedType.of(packageName, receiver));
		} else {
			// TODO: there must be a better way
			final int lastDot = receiver.lastIndexOf(DOT);
			types.add(ReferencedType.of(
					receiver.substring(0, lastDot),
					receiver.substring(lastDot + 1)
			));
		}
	}

	private boolean isVariable(String name) {
		return variables.stream().anyMatch(variablesInBlock -> variablesInBlock.contains(name));
	}

	private ReferencedType toReferencedType(List<TerminalNode> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (nodes.size() == 1) {
			return ReferencedType.of(packageName, nodes.get(0).getText());
		}
		// TODO:
		final List<String> packageNodes = nodes.stream()
				.filter(this::startsWithLower)
				.map(TerminalNode::getText)
				.collect(Collectors.toList());

		final List<String> typeNodes = nodes.stream()
				.filter(this::startsWithUpper)
				.map(TerminalNode::getText)
				.collect(Collectors.toList());

		final PackageDeclaration thePackageName = (packageNodes.isEmpty())
				? packageName
				: new PackageDeclaration(String.join(DOT, packageNodes));

		return ReferencedType.of(thePackageName, typeNodes.get(0));
	}

	private boolean startsWithLower(ParseTree hasText) {
		return Character.isLowerCase(hasText.getText().charAt(0));
	}

	private boolean startsWithUpper(ParseTree hasText) {
		return Character.isUpperCase(hasText.getText().charAt(0));
	}

	public Set<String> getTypes() {
		return types.stream().map(ReferencedType::toQualifiedName).collect(Collectors.toSet());
	}
}
