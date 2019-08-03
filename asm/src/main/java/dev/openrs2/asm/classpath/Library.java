package dev.openrs2.asm.classpath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
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
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Library implements Iterable<ClassNode> {
	private static final Logger logger = LoggerFactory.getLogger(Library.class);

	private static final String CLASS_SUFFIX = ".class";
	private static final String TEMP_PREFIX = "tmp";
	private static final String JAR_SUFFIX = ".jar";
	private static final byte[] GZIP_HEADER = { 0x1f, (byte) 0x8b };

	public static Library readJar(Path path) throws IOException {
		logger.info("Reading jar {}", path);

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

				library.add(clazz);
			}
		}

		return library;
	}

	public static Library readPack(Path path) throws IOException {
		logger.info("Reading pack {}", path);

		var temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX);
		try {
			try (
				var header = new ByteArrayInputStream(GZIP_HEADER);
				var data = Files.newInputStream(path);
				var in = new GZIPInputStream(new SequenceInputStream(header, data));
				var out = new JarOutputStream(Files.newOutputStream(temp))
			) {
				Pack200.newUnpacker().unpack(in, out);
				return readJar(temp);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private final Map<String, ClassNode> classes = new TreeMap<>();

	public Library() {
		/* empty */
	}

	public Library(Library library) {
		for (var clazz : library.classes.values()) {
			var copy = new ClassNode();
			clazz.accept(copy);
			add(copy);
		}
	}

	public boolean contains(String name) {
		return classes.containsKey(name);
	}

	public ClassNode get(String name) {
		return classes.get(name);
	}

	public ClassNode add(ClassNode clazz) {
		return classes.put(clazz.name, clazz);
	}

	public ClassNode remove(String name) {
		return classes.remove(name);
	}

	@Override
	public Iterator<ClassNode> iterator() {
		return classes.values().iterator();
	}

	public void writeJar(Path path, Remapper remapper) throws IOException {
		logger.info("Writing jar {}", path);

		try (var out = new DeterministicJarOutputStream(Files.newOutputStream(path))) {
			for (var clazz : classes.values()) {
				var name = clazz.name;
				var writer = new ClassWriter(0);

				ClassVisitor visitor = new CheckClassAdapter(writer, true);
				if (remapper != null) {
					visitor = new ClassRemapper(visitor, remapper);
					name = remapper.map(name);
				}
				clazz.accept(visitor);

				out.putNextEntry(new JarEntry(name + CLASS_SUFFIX));
				out.write(writer.toByteArray());
			}
		}
	}

	public void writePack(Path path, Remapper remapper) throws IOException {
		logger.info("Writing pack {}", path);

		var temp = Files.createTempFile(TEMP_PREFIX, JAR_SUFFIX);
		try {
			writeJar(temp, remapper);

			try (
				var in = new JarInputStream(Files.newInputStream(temp));
				var out = new GZIPOutputStream(new SkipOutputStream(Files.newOutputStream(path), 2))
			) {
				Pack200.newPacker().pack(in, out);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
