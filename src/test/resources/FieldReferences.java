package foo;

public class MethodReferences {
	private String c = C.CONSTANT_SAME_PACKAGE;
	private String d = bar.D.CONSTANT_OTHER_PACKAGE;

	public void m() {
		String a = A.CONSTANT_SAME_PACKAGE;
		String b = bar.B.CONSTANT_OTHER_PACKAGE;
	}
}
