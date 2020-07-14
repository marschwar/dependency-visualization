package foo;


public class IgnoreInner {

	private InnerA innerA = new InnerA();
	private InnerInterface innerB = new InnerB();
	private E e = E.X;

	public class InnerA {

	}

	public interface InnerInterface {

	}

	private static class InnerB implements InnerInterface {

	}

	public enum E {
		X;
	}
}
