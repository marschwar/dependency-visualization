package foo;

import bar.Other.Inner;
import bar.Other.Inner.CONSTANT;

public class MethodReferences {

	private Object c = C.getInstance().foo();
	private Object d = bar.D.getInstance();

	public void m() {
		{
			final A a = A.typeInSamePackage();
			a.methodCall();
		}

		bar.B.fullyQualified();

		this.ignoreLocal();

		c.some().other();
	}

	private void ignoreLocal() {
		Inner.getString(CONSTANT);
	}
}
