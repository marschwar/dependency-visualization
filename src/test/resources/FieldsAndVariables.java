package foo;


public class ImportedTypes {

	private static final Integer INTEGER = 1;
	private static final long LONG = 1L;
	static final String[] ARRAY = new String[]{"a", "b"};
	A a = null;

	public void method() {
		B b = null;
		bar.C c = null;
		String s = "";
		double d = 42.0;

		for (String value : ARRAY) {
		}
		String[] copy = ARRAY;

	}
}
