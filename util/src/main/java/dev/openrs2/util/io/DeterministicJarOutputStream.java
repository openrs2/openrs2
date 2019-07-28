package dev.openrs2.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public final class DeterministicJarOutputStream extends JarOutputStream {
	private static final FileTime UNIX_EPOCH = FileTime.fromMillis(0);

	public DeterministicJarOutputStream(OutputStream out) throws IOException {
		super(out);
	}

	public DeterministicJarOutputStream(OutputStream out, Manifest man) throws IOException {
		super(out, man);
	}

	@Override
	public void putNextEntry(ZipEntry ze) throws IOException {
		ze.setCreationTime(UNIX_EPOCH);
		ze.setLastAccessTime(UNIX_EPOCH);
		ze.setLastModifiedTime(UNIX_EPOCH);
		super.putNextEntry(ze);
	}
}
