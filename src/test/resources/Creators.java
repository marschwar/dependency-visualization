package foo;

public class Creators {

	public void m() {
		new A().doSomething();
		new bar.B();
		new C.Inner();

		// C::new
	}
}
