package foo;

public class Creators {

	public void m() {
		new A().doSomething();
		new foo.B();

		// C::new
	}
}
