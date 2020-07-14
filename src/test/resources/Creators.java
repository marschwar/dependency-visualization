package foo;

import java.util.function.Supplier;
import bar.Outer;

public class Creators {

	public void m() {
		new A().doSomething();
		new bar.B();
		new C.Inner();

		Supplier s1 = D::new;
		Supplier s2 = bar.E::new;
		Supplier s3 = String::new;

		Outer.Inner inner = new Outer.Inner();
	}
}
