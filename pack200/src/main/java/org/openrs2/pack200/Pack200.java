package org.openrs2.pack200;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

public abstract class Pack200 {
	public interface Unpacker {
		void unpack(InputStream in, JarOutputStream out) throws IOException;
	}

	public static Unpacker newUnpacker() {
		throw new UnsupportedOperationException();
	}

	private Pack200() {
		// empty
	}
}
