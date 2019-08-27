package dev.openrs2.deob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.collect.ImmutableList;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.bundler.transform.HostCheckTransformer;
import dev.openrs2.bundler.transform.RightClickTransformer;
import dev.openrs2.deob.remap.ClassNamePrefixer;
import dev.openrs2.deob.remap.TypedRemapper;
import dev.openrs2.deob.transform.BitShiftTransformer;
import dev.openrs2.deob.transform.BitwiseOpTransformer;
import dev.openrs2.deob.transform.CanvasTransformer;
import dev.openrs2.deob.transform.ClassForNameTransformer;
import dev.openrs2.deob.transform.CounterTransformer;
import dev.openrs2.deob.transform.DummyLocalTransformer;
import dev.openrs2.deob.transform.DummyArgTransformer;
import dev.openrs2.deob.transform.ExceptionTracingTransformer;
import dev.openrs2.deob.transform.FieldOrderTransformer;
import dev.openrs2.deob.transform.OpaquePredicateTransformer;
import dev.openrs2.deob.transform.OriginalNameTransformer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Deobfuscator {
	private static final Logger logger = LoggerFactory.getLogger(Deobfuscator.class);

	private static final ImmutableList<Transformer> TRANSFORMERS = ImmutableList.of(
		new OriginalNameTransformer(),
		new HostCheckTransformer(),
		new RightClickTransformer(),
		new OpaquePredicateTransformer(),
		new ExceptionTracingTransformer(),
		new BitShiftTransformer(),
		new CounterTransformer(),
		new CanvasTransformer(),
		new FieldOrderTransformer(),
		new BitwiseOpTransformer(),
		new DummyArgTransformer(),
		new DummyLocalTransformer()
	);

	public static void main(String[] args) throws IOException, AnalyzerException {
		var deobfuscator = new Deobfuscator(Paths.get("nonfree/code"), Paths.get("nonfree/code/deob"));
		deobfuscator.run();
	}

	private final Path input, output;

	public Deobfuscator(Path input, Path output) {
		this.input = input;
		this.output = output;
	}

	public void run() throws IOException, AnalyzerException {
		/* read input jars/packs */
		logger.info("Reading input jars");
		var unpacker = Library.readJar(input.resolve("game_unpacker.dat"));
		var glUnpacker = new Library(unpacker);
		var loader = Library.readJar(input.resolve("loader.jar"));
		var glLoader = Library.readJar(input.resolve("loader_gl.jar"));
		var gl = Library.readPack(input.resolve("jaggl.pack200"));
		var client = Library.readJar(input.resolve("runescape.jar"));
		var glClient = Library.readPack(input.resolve("runescape_gl.pack200"));

		// TODO(gpe): it'd be nice to have separate signlink.jar and
		// signlink-unsigned.jar files so we don't (effectively) deobfuscate
		// runescape.jar twice with different sets of names, but thinking about
		// how this would work is tricky (as the naming must match)
		var unsignedClient = new Library(client);

		/* overwrite client's classes with signed classes from the loader */
		logger.info("Moving signed classes from loader");
		var signLink = new Library();
		SignedClassUtils.move(loader, client, signLink);

		logger.info("Moving signed classes from loader_gl");
		var glSignLink = new Library();
		SignedClassUtils.move(glLoader, glClient, glSignLink);

		/* move unpack class out of the loader (so the unpacker and loader can both depend on it) */
		logger.info("Moving unpack from loader to unpack");
		var unpack = new Library();
		unpack.add(loader.remove("unpack"));

		logger.info("Moving unpack from loader_gl to unpack_gl");
		var glUnpack = new Library();
		glUnpack.add(glLoader.remove("unpack"));

		/* move DRI classes out of jaggl (so we can place javah-generated headers in a separate directory) */
		logger.info("Moving DRI classes from jaggl to jaggl_dri");
		var glDri = new Library();
		glDri.add(gl.remove("com/sun/opengl/impl/x11/DRIHack"));
		glDri.add(gl.remove("com/sun/opengl/impl/x11/DRIHack$1"));
		glDri.add(gl.remove("jaggl/X11/dri"));

		/* prefix remaining loader/unpacker classes (to avoid conflicts when we rename in the same classpath as the client) */
		logger.info("Prefixing loader and unpacker class names");
		ClassNamePrefixer.addPrefix(loader, "loader_");
		ClassNamePrefixer.addPrefix(glLoader, "loader_");
		ClassNamePrefixer.addPrefix(unpacker, "unpacker_");
		ClassNamePrefixer.addPrefix(glUnpacker, "unpacker_");

		/* bundle libraries together into a common classpath */
		var runtime = ClassLoader.getPlatformClassLoader();
		var classPath = new ClassPath(runtime, ImmutableList.of(), ImmutableList.of(client, loader, signLink, unpack, unpacker));
		var glClassPath = new ClassPath(runtime, ImmutableList.of(gl, glDri), ImmutableList.of(glClient, glLoader, glSignLink, glUnpack, glUnpacker));
		var unsignedClassPath = new ClassPath(runtime, ImmutableList.of(), ImmutableList.of(unsignedClient));

		/* deobfuscate */
		logger.info("Transforming client");
		for (var transformer : TRANSFORMERS) {
			logger.info("Running transformer {}", transformer.getClass().getSimpleName());
			transformer.transform(classPath);
		}

		logger.info("Transforming client_gl");
		for (var transformer : TRANSFORMERS) {
			logger.info("Running transformer {}", transformer.getClass().getSimpleName());
			transformer.transform(glClassPath);
		}

		logger.info("Transforming client_unsigned");
		for (var transformer : TRANSFORMERS) {
			logger.info("Running transformer {}", transformer.getClass().getSimpleName());
			transformer.transform(unsignedClassPath);
		}

		/* remap all class, method and field names */
		logger.info("Creating remappers");
		var remapper = TypedRemapper.create(classPath);
		var glRemapper = TypedRemapper.create(glClassPath);
		var unsignedRemapper = TypedRemapper.create(unsignedClassPath);

		/* transform Class.forName() calls */
		logger.info("Transforming Class.forName() calls");
		Transformer transformer = new ClassForNameTransformer(remapper);
		transformer.transform(classPath);

		transformer = new ClassForNameTransformer(glRemapper);
		transformer.transform(glClassPath);

		transformer = new ClassForNameTransformer(unsignedRemapper);
		transformer.transform(unsignedClassPath);

		/* write output jars */
		logger.info("Writing output jars");

		Files.createDirectories(output);

		client.writeJar(output.resolve("runescape.jar"), remapper);
		loader.writeJar(output.resolve("loader.jar"), remapper);
		signLink.writeJar(output.resolve("signlink.jar"), remapper);
		unpack.writeJar(output.resolve("unpack.jar"), remapper);
		unpacker.writeJar(output.resolve("unpacker.jar"), remapper);

		gl.writeJar(output.resolve("jaggl.jar"), glRemapper);
		glDri.writeJar(output.resolve("jaggl_dri.jar"), glRemapper);
		glClient.writeJar(output.resolve("runescape_gl.jar"), glRemapper);
		glLoader.writeJar(output.resolve("loader_gl.jar"), glRemapper);
		glSignLink.writeJar(output.resolve("signlink_gl.jar"), glRemapper);
		glUnpack.writeJar(output.resolve("unpack_gl.jar"), glRemapper);
		glUnpacker.writeJar(output.resolve("unpacker_gl.jar"), glRemapper);

		unsignedClient.writeJar(output.resolve("runescape_unsigned.jar"), unsignedRemapper);
	}
}
