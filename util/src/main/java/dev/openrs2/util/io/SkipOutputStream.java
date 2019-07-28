package dev.openrs2.util.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class SkipOutputStream extends FilterOutputStream {
	private long skipBytes;

	public SkipOutputStream(OutputStream out, long skipBytes) {
		super(out);
		this.skipBytes = skipBytes;
	}

	@Override
	public void write(int b) throws IOException {
		if (skipBytes == 0) {
			super.write(b);
		} else {
			skipBytes--;
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (len >= skipBytes) {
			off += skipBytes;
			len -= skipBytes;
			skipBytes = 0;
		} else {
			skipBytes -= len;
			return;
		}

		super.write(b, off, len);
	}
}
