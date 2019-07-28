package dev.openrs2.asm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import dev.openrs2.util.io.DeterministicJarOutputStream;
import dev.openrs2.util.io.SkipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public final class Library {
	private static final String CLASS_SUFFIX = ".class";
	private static final String TEMP_PREFIX = "tmp";
	private static final String JAR_SUFFIX = ".jar";
	private static final byte[] GZIP_HEADER = { 0x1f, (byte) 0x8b };

	public static Library readJar(Path path) throws IOException {
		var library = new Library();

		try (var in = new JarInputStream(Files.newInputStream(path))) {
			JarEntry entry;
			while ((entry = in.getNextJarEntry()) != null) {
				if (!entry.getName().endsWith(CLASS_SUFFIX)) {
					continue;
				}

				var clazz = new ClassNode();
				var reader = new ClassReader(in);
				reader.accept(clazz, ClassReader.SKIP_DEBUG);

				library.classes.put(clazz.name, clazz);
			}
		}

		return library;
	}

	public static Library readPack(Path path) throws IOException {
		var temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX);
		try {
			try (var header = new ByteArrayInputStream(GZIP_HEADER);
					var data = Files.newInputStream(path);
					var in = new GZIPInputStream(new SequenceInputStream(header, data));
					var out = new JarOutputStream(Files.newOutputStream(temp))) {
				Pack200.newUnpacker().unpack(in, out);
				return readJar(temp);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private final Map<String, ClassNode> classes = new TreeMap<>();

	private Library() {
		/* empty */
	}

	public void writeJar(Path path) throws IOException {
		try (var out = new DeterministicJarOutputStream(Files.newOutputStream(path))) {
			for (var entry : classes.entrySet()) {
				var clazz = entry.getValue();
				var writer = new ClassWriter(0);
				clazz.accept(new CheckClassAdapter(writer, true));

				out.putNextEntry(new JarEntry(clazz.name + CLASS_SUFFIX));
				out.write(writer.toByteArray());
			}
		}
	}

	public void writePack(Path path) throws IOException {
		var temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX);
		try {
			writeJar(temp);

			try (var in = new JarInputStream(Files.newInputStream(temp));
					var out = new GZIPOutputStream(new SkipOutputStream(Files.newOutputStream(path), 2))) {
				Pack200.newPacker().pack(in, out);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
