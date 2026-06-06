package org.openrs2.jsobject;

import java.applet.Applet;

public abstract class JSObject {
	public static JSObject getWindow(Applet applet) {
		return null;
	}

	protected JSObject() {
		// empty
	}

	public abstract Object call(String methodName, Object... args);
	public abstract Object eval(String script);
}
