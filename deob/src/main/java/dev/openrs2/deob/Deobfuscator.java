package dev.openrs2.deob;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import dev.openrs2.asm.Library;
import dev.openrs2.asm.Transformer;
import dev.openrs2.deob.classpath.ClassPath;
import dev.openrs2.deob.classpath.TypedRemapper;
import dev.openrs2.deob.transform.CanvasTransformer;
import dev.openrs2.deob.transform.ClassForNameTransformer;
import dev.openrs2.deob.transform.CounterTransformer;
import dev.openrs2.deob.transform.ExceptionTracingTransformer;
import dev.openrs2.deob.transform.OpaquePredicateTransformer;
import dev.openrs2.deob.transform.OriginalNameTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Deobfuscator {
	private static final Logger logger = LoggerFactory.getLogger(Deobfuscator.class);

	private static final List<Transformer> TRANSFORMERS = List.of(
		new OpaquePredicateTransformer(),
		new ExceptionTracingTransformer(),
		new CounterTransformer(),
		new CanvasTransformer()
	);

	public static void main(String[] args) throws IOException {
		var deobfuscator = new Deobfuscator(Paths.get("nonfree/code"), Paths.get("nonfree/code/deob"));
		deobfuscator.run();
	}

	private final Path input, output;

	public Deobfuscator(Path input, Path output) {
		this.input = input;
		this.output = output;
	}

	public void run() throws IOException {
		/* read input jars/packs */
		logger.info("Reading input jars");
		var unpacker = Library.readJar(input.resolve("game_unpacker.dat"));
		var glUnpacker = new Library(unpacker);
		var loader = Library.readJar(input.resolve("loader.jar"));
		var glLoader = Library.readJar(input.resolve("loader_gl.jar"));
		var client = Library.readJar(input.resolve("runescape.jar"));
		var glClient = Library.readPack(input.resolve("runescape_gl.pack200"));
		var unsignedClient = new Library(client);

		/* read dependencies */
		var runtime = ClassLoader.getPlatformClassLoader();
		var jogl = new URLClassLoader(new URL[] {
			input.resolve("jogl.jar").toUri().toURL()
		}, runtime);

		/* overwrite client's classes with signed classes from the loader */
		logger.info("Moving signed classes from loader to runescape");
		var signedClasses = SignedClassSet.create(loader, client);

		logger.info("Moving signed classes from loader_gl to runescape_gl");
		var glSignedClasses = SignedClassSet.create(glLoader, glClient);

		/* deobfuscate */
		var allLibraries = Map.of(
			"unpacker", unpacker,
			"unpacker_gl", glUnpacker,
			"loader", loader,
			"loader_gl", glLoader,
			"runescape", client,
			"runescape_gl", glClient,
			"runescape_unsigned", unsignedClient
		);

		for (var entry : allLibraries.entrySet()) {
			logger.info("Transforming library {}", entry.getKey());

			for (var transformer : TRANSFORMERS) {
				logger.info("Running transformer {}", transformer.getClass().getSimpleName());
				transformer.transform(entry.getValue());
			}
		}

		/* move unpack class out of the loader (so the unpacker and loader can both depend on it) */
		logger.info("Moving unpack from loader to unpack");
		var unpack = new Library();
		unpack.add(loader.remove("unpack"));

		logger.info("Moving unpack from loader_gl to unpack_gl");
		var glUnpack = new Library();
		glUnpack.add(glLoader.remove("unpack"));

		/* move signed classes out of the client (so the client and loader can both depend on them) */
		logger.info("Moving signed classes from runescape to signlink");
		var signLink = new Library();
		signedClasses.move(client, signLink);

		logger.info("Moving signed classes from runescape_gl to signlink_gl");
		var glSignLink = new Library();
		glSignedClasses.move(glClient, glSignLink);

		/* prefix remaining loader/unpacker classes (to avoid conflicts when we rename in the same classpath as the client) */
		logger.info("Prefixing loader and unpacker class names");
		ClassNamePrefixer.addPrefix(loader, "loader_");
		ClassNamePrefixer.addPrefix(glLoader, "loader_");
		ClassNamePrefixer.addPrefix(unpacker, "unpacker_");
		ClassNamePrefixer.addPrefix(glUnpacker, "unpacker_");

		/* remap all class, method and field names */
		logger.info("Creating remappers");
		var libraries = new Library[] { client, loader, signLink, unpack, unpacker };
		var remapper = TypedRemapper.create(new ClassPath(runtime, libraries));

		var glLibraries = new Library[] { glClient, glLoader, glSignLink, glUnpack, glUnpacker };
		var glRemapper = TypedRemapper.create(new ClassPath(jogl, glLibraries));

		var unsignedRemapper = TypedRemapper.create(new ClassPath(runtime, unsignedClient));

		/* transform Class.forName() calls */
		logger.info("Transforming Class.forName() calls");
		Transformer transformer = new ClassForNameTransformer(remapper);
		for (var library : libraries) {
			transformer.transform(library);
		}

		transformer = new ClassForNameTransformer(glRemapper);
		for (var library : glLibraries) {
			transformer.transform(library);
		}

		transformer = new ClassForNameTransformer(unsignedRemapper);
		transformer.transform(unsignedClient);

		/* add @OriginalName annotations */
		logger.info("Annotating classes and members with original names");
		transformer = new OriginalNameTransformer();
		for (var library : libraries) {
			transformer.transform(library);
		}

		for (var library : glLibraries) {
			transformer.transform(library);
		}

		transformer.transform(unsignedClient);

		/* write output jars */
		logger.info("Writing output jars");

		Files.createDirectories(output);

		client.writeJar(output.resolve("runescape.jar"), remapper);
		loader.writeJar(output.resolve("loader.jar"), remapper);
		signLink.writeJar(output.resolve("signlink.jar"), remapper);
		unpack.writeJar(output.resolve("unpack.jar"), remapper);
		unpacker.writeJar(output.resolve("unpacker.jar"), remapper);

		glClient.writeJar(output.resolve("runescape_gl.jar"), glRemapper);
		glLoader.writeJar(output.resolve("loader_gl.jar"), glRemapper);
		glSignLink.writeJar(output.resolve("signlink_gl.jar"), glRemapper);
		glUnpack.writeJar(output.resolve("unpack_gl.jar"), glRemapper);
		glUnpacker.writeJar(output.resolve("unpacker_gl.jar"), glRemapper);

		unsignedClient.writeJar(output.resolve("runescape_unsigned.jar"), unsignedRemapper);
	}
}
