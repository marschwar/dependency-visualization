package foo;

public class MethodReferences {
	private String c = C.CONSTANT_SAME_PACKAGE;
	private String d = foo.bar.D.CONSTANT_OTHER_PACKAGE.first();
	private String d = "aString";

	public void m() {
		String a = A.CONSTANT_SAME_PACKAGE;
		String b = bar.B.CONSTANT_OTHER_PACKAGE;
		int i = 4 + 5;
	}
}
