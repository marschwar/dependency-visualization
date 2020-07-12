package foo;

import bar.A;

import java.util.Arrays;
import java.util.List;

public class ImportedTypes {
	A a = new A();

	List<String> list = Arrays.asList("");

	Runnable r = new Runnable() {
		@Override
		public void run() {
			// noop
		}
	};
}
