package foo;


import java.util.Arrays;
import java.util.List;

public class Constants {

	private static final List<String> NAMES = Arrays.asList("Foo", "Bar");

	public void method() {
		NAMES.forEach(name -> null);
	}
}
