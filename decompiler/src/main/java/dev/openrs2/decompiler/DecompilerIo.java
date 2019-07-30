package dev.openrs2.decompiler;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.google.common.io.ByteStreams;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

public final class DecompilerIo implements IBytecodeProvider, IResultSaver, Closeable {
	private final Map<String, JarFile> inputJars = new HashMap<>();
	private final Function<String, Path> destination;

	public DecompilerIo(Function<String, Path> destination) {
		this.destination = destination;
	}

	@Override
	public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
		if (internalPath == null) {
			throw new UnsupportedOperationException();
		}

		var jar = inputJars.get(externalPath);
		if (jar == null) {
			jar = new JarFile(externalPath);
			inputJars.put(externalPath, jar);
		}

		try (var in = jar.getInputStream(jar.getJarEntry(internalPath))) {
			return ByteStreams.toByteArray(in);
		}
	}

	@Override
	public void saveFolder(String path) {
		/* ignore */
	}

	@Override
	public void copyFile(String source, String path, String entryName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void createArchive(String path, String archiveName, Manifest manifest) {
		/* ignore */
	}

	@Override
	public void saveDirEntry(String path, String archiveName, String entryName) {
		/* ignore */
	}

	@Override
	public void copyEntry(String source, String path, String archiveName, String entry) {
		/* ignore */
	}

	@Override
	public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
		var p = destination.apply(archiveName).resolve(entryName);
		try {
			Files.createDirectories(p.getParent());
			try (var writer = Files.newBufferedWriter(p)) {
				writer.write(content);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void closeArchive(String path, String archiveName) {
		/* ignore */
	}

	@Override
	public void close() throws IOException {
		for (var jar : inputJars.values()) {
			jar.close();
		}
	}
}
