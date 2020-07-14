package com.github.marschwar.classDependency;

import com.github.marschwar.classDependency.parser.JavaParser;
import com.github.marschwar.classDependency.parser.JavaParser.ClassDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.ClassOrInterfaceTypeContext;
import com.github.marschwar.classDependency.parser.JavaParser.CreatedNameContext;
import com.github.marschwar.classDependency.parser.JavaParser.ExpressionContext;
import com.github.marschwar.classDependency.parser.JavaParser.ImportDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.InterfaceDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.LocalVariableDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.MethodCallContext;
import com.github.marschwar.classDependency.parser.JavaParser.PackageDeclarationContext;
import com.github.marschwar.classDependency.parser.JavaParser.TypeTypeContext;
import com.github.marschwar.classDependency.parser.JavaParser.VariableInitializerContext;
import com.github.marschwar.classDependency.parser.JavaParserBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ReferencedTypesListener extends JavaParserBaseListener {

	private static final String DOT = ".";
	private static final PackageDeclaration JAVA_LANG_PACKAGE = new PackageDeclaration("java.lang");

	private static final List<String> JAVA_LANG_TYPES = Arrays.asList("AbstractMethodError",
			"AbstractStringBuilder",
			"Appendable",
			"ApplicationShutdownHooks",
			"ArithmeticException",
			"ArrayIndexOutOfBoundsException",
			"ArrayStoreException",
			"AssertionError",
			"AssertionStatusDirectives",
			"AutoCloseable",
			"Boolean",
			"BootstrapMethodError",
			"Byte",
			"Character",
			"CharacterData",
			"CharacterData0E",
			"CharacterData00",
			"CharacterData01",
			"CharacterData02",
			"CharacterDataLatin1",
			"CharacterDataPrivateUse",
			"CharacterDataUndefined",
			"CharacterName",
			"CharSequence",
			"Class",
			"ClassCastException",
			"ClassCircularityError",
			"ClassFormatError",
			"ClassLoader",
			"ClassLoaderHelper",
			"ClassNotFoundException",
			"ClassValue",
			"Cloneable",
			"CloneNotSupportedException",
			"Comparable",
			"Compiler",
			"ConditionalSpecialCasing",
			"Deprecated",
			"Double",
			"Enum",
			"EnumConstantNotPresentException",
			"Error",
			"Exception",
			"ExceptionInInitializerError",
			"Float",
			"FunctionalInterface",
			"IllegalAccessError",
			"IllegalAccessException",
			"IllegalArgumentException",
			"IllegalMonitorStateException",
			"IllegalStateException",
			"IllegalThreadStateException",
			"IncompatibleClassChangeError",
			"IndexOutOfBoundsException",
			"InheritableThreadLocal",
			"InstantiationError",
			"InstantiationException",
			"Integer",
			"InternalError",
			"InterruptedException",
			"Iterable",
			"LinkageError",
			"Long",
			"Math",
			"NegativeArraySizeException",
			"NoClassDefFoundError",
			"NoSuchFieldError",
			"NoSuchFieldException",
			"NoSuchMethodError",
			"NoSuchMethodException",
			"NullPointerException",
			"Number",
			"NumberFormatException",
			"Object",
			"OutOfMemoryError",
			"Override",
			"Package",
			"Process",
			"ProcessBuilder",
			"ProcessEnvironment",
			"ProcessImpl",
			"Readable",
			"ReflectiveOperationException",
			"Runnable",
			"Runtime",
			"RuntimeException",
			"RuntimePermission",
			"SafeVarargs",
			"SecurityException",
			"SecurityManager",
			"Short",
			"Shutdown",
			"StackOverflowError",
			"StackTraceElement",
			"StrictMath",
			"String",
			"StringBuffer",
			"StringBuilder",
			"StringCoding",
			"StringIndexOutOfBoundsException",
			"SuppressWarnings",
			"System",
			"SystemClassLoaderAction",
			"Terminator",
			"Thread",
			"ThreadDeath",
			"ThreadGroup",
			"ThreadLocal",
			"Throwable",
			"TypeNotPresentException",
			"UNIXProcess",
			"UnknownError",
			"UnsatisfiedLinkError",
			"UnsupportedClassVersionError",
			"UnsupportedOperationException",
			"VerifyError",
			"VirtualMachineError",
			"Void");

	private PackageDeclaration packageName;
	private String typeName;
	private Set<String> localTypes = new HashSet<>();

	private final Set<ReferencedType> types = new HashSet<>();
	private final Stack<Set<String>> variables = new Stack<>();
	private final Set<String> imports = new HashSet<>();

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		packageName = new PackageDeclaration(ctx.qualifiedName().getText());
	}


	@Override
	public void enterClassDeclaration(ClassDeclarationContext ctx) {
		onLocalTypeName(ctx.IDENTIFIER());
		onVariableContextStart();
	}

	@Override
	public void exitClassDeclaration(ClassDeclarationContext ctx) {
		onVariableContextEnd();
	}

	@Override
	public void enterInterfaceDeclaration(InterfaceDeclarationContext ctx) {
		onLocalTypeName(ctx.IDENTIFIER());
		onVariableContextStart();
	}

	@Override
	public void exitInterfaceDeclaration(InterfaceDeclarationContext ctx) {
		onVariableContextEnd();
	}

	@Override
	public void enterEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
		onLocalTypeName(ctx.IDENTIFIER());
		onVariableContextStart();
	}

	@Override
	public void exitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) {
		onVariableContextEnd();
	}

	private void onLocalTypeName(TerminalNode classOrInterface) {
		String classOrInterfaceName = classOrInterface.getText();
		if (typeName == null) {
			typeName = classOrInterfaceName;
		}
		localTypes.add(classOrInterfaceName);
		types.remove(ReferencedType.of(packageName, classOrInterfaceName));
	}

	@Override
	public void enterBlock(JavaParser.BlockContext ctx) {
		onVariableContextStart();
	}

	@Override
	public void exitBlock(JavaParser.BlockContext ctx) {
		onVariableContextEnd();
	}

	private void onVariableContextStart() {
		variables.push(new HashSet<>());
	}

	private void onVariableContextEnd() {
		variables.pop();
	}

	@Override
	public void enterVariableDeclaratorId(JavaParser.VariableDeclaratorIdContext ctx) {
		variables.peek().add(ctx.IDENTIFIER().getText());
	}

	@Override
	public void enterImportDeclaration(ImportDeclarationContext ctx) {
		if (ctx.STATIC() == null && ctx.MUL() != null) {
			// wildcard imports are not supported
			return;
		}
		List<TerminalNode> nodes = ctx.qualifiedName().IDENTIFIER();

		if (ctx.STATIC() != null && ctx.MUL() == null) {
			// remove method name in static method import
			nodes = nodes.subList(0, nodes.size() - 1);
		}
		final ReferencedType typeOrNull = toReferencedTypeOrNull(nodes);
		addToTypes(typeOrNull);

		nodes.stream()
				.filter(this::startsWithUpper)
				.map(TerminalNode::getText)
				.forEach(imports::add);
	}

	@Override
	public void enterCreatedName(CreatedNameContext ctx) {
		if (ctx.primitiveType() != null) {
			return;
		}
		final ReferencedType typeOrNull = toReferencedTypeOrNull(ctx.IDENTIFIER());
		addToTypes(typeOrNull);
	}

	@Override
	public void enterVariableInitializer(VariableInitializerContext ctx) {

		final ExpressionContext expression = ctx.expression();
		if (expression != null) {
			final ReferencedType referencedTypeOrNull = toReferencedTypeOrNull(expression);
			addToTypes(referencedTypeOrNull);
		}
	}

	@Override
	public void enterMethodCall(MethodCallContext ctx) {
		final ExpressionContext parent = (ExpressionContext) ctx.getParent();
		final ReferencedType referencedTypeOrNull = toReferencedTypeOrNull(parent);
		addToTypes(referencedTypeOrNull);
	}

	@Override
	public void enterExpression(ExpressionContext ctx) {
		onTypeType(ctx.typeType());
	}

	@Override
	public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
		onTypeType(ctx.typeType());
	}

	@Override
	public void enterLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {
		onTypeType(ctx.typeType());
	}

	private void onTypeType(TypeTypeContext typeType) {
		if (typeType == null) {
			return;
		}
		final ClassOrInterfaceTypeContext classOrInterfaceType = typeType.classOrInterfaceType();
		if (classOrInterfaceType == null) {
			return;
		}
		final ReferencedType typeOrNull = toReferencedTypeOrNull(classOrInterfaceType.IDENTIFIER());
		addToTypes(typeOrNull);
		classOrInterfaceType.typeArguments().forEach(argsCtx -> {
			argsCtx.typeArgument().forEach(typeArgCtx -> {
				final TypeTypeContext parametrizedTypeType = typeArgCtx.typeType();
				onTypeType(parametrizedTypeType);
			});
		});
	}

	private boolean isVariable(String name) {
		return variables.stream().anyMatch(variablesInBlock -> variablesInBlock.contains(name));
	}

	private boolean isImported(String name) {
		return imports.contains(name);
	}

	private boolean isJavaLangType(String name) {
		return JAVA_LANG_TYPES.stream().anyMatch(type -> type.equals(name));
	}

	private ReferencedType toReferencedTypeOrNull(ExpressionContext ctx) {
		final List<ExpressionContext> expressions = ctx.expression();
		final TerminalNode typeOrPackageOrNull = typeOrPackageOrNull(ctx);
		return toReferencedTypeOrNull(expressions, typeOrPackageOrNull);
	}

	private ReferencedType toReferencedTypeOrNull(List<ExpressionContext> expressions, TerminalNode typeCandidate) {
		if (expressions.isEmpty()) {
			if (typeCandidate != null) {
				return toReferencedTypeOrNull(typeCandidate);
			}
			return null;
		}

		// TODO: multiple?
		final ExpressionContext firstExpression = expressions.get(0);
		final TerminalNode typeOrPackageOrNull = typeOrPackageOrNull(firstExpression);
		if (typeOrPackageOrNull == null) {
			return (typeCandidate == null)
					? toReferencedTypeOrNull(firstExpression.expression(), null)
					: toReferencedTypeOrNull(typeCandidate);

		}
		if (isVariable(typeOrPackageOrNull.getText())) {
			return null;
		}
		if (startsWithUpper(typeOrPackageOrNull)) {
			return toReferencedTypeOrNull(firstExpression.expression(), typeOrPackageOrNull);
		}
		final String packageCandidate = firstExpression.getText();
		if (isVariable(packageCandidate)) {
			return null;
		}
		if (typeCandidate == null) {
			return null;
		}
		return ReferencedType.of(packageCandidate, typeCandidate.getText());
	}

	private TerminalNode typeOrPackageOrNull(ExpressionContext ctx) {
		if (ctx.IDENTIFIER() != null) {
			return ctx.IDENTIFIER();
		}
		if (ctx.primary() != null) {
			return ctx.primary().IDENTIFIER();
		}
		return null;
	}

	private ReferencedType toReferencedTypeOrNull(List<TerminalNode> nodes) {
		if (nodes == null || nodes.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (nodes.size() == 1) {
			return toReferencedTypeOrNull(nodes.get(0));
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

	private ReferencedType toReferencedTypeOrNull(TerminalNode node) {
		final String typeNameCandidate = node.getText();
		if (isImported(typeNameCandidate)) {
			return null;
		}
		if (isVariable(typeNameCandidate)) {
			return null;
		}
		if (localTypes.contains(typeNameCandidate)) {
			return null;
		}
		return (isJavaLangType(typeNameCandidate))
				? ReferencedType.of(JAVA_LANG_PACKAGE, typeNameCandidate)
				: ReferencedType.of(packageName, typeNameCandidate);
	}

	private void addToTypes(ReferencedType referencedTypeOrNull) {
		if (referencedTypeOrNull == null) {
			return;
		}
		types.add(referencedTypeOrNull);
	}

	Set<String> getTypes() {
		return types.stream().map(ReferencedType::getQualifiedName).collect(Collectors.toSet());
	}

	public TypeReport createReport() {
		return TypeReport.builder()
				.name(typeName)
				.packageDeclaration(packageName)
				.referencedTypes(new ArrayList<>(types))
				.build();
	}
}
