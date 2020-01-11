package netscape.javascript;

import java.applet.Applet;

public abstract class JSObject {
	public static JSObject getWindow(Applet applet) {
		return null;
	}

	public abstract Object call(String methodName, Object... args);
	public abstract Object eval(String s);
}
