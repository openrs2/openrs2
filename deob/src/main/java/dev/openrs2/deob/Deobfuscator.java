package dev.openrs2.deob;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import dev.openrs2.asm.Library;
import dev.openrs2.asm.Transformer;
import dev.openrs2.deob.transform.OpaquePredicateTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Deobfuscator {
	private static final Logger logger = LoggerFactory.getLogger(Deobfuscator.class);

	private static final List<Transformer> TRANSFORMERS = List.of(
		new OpaquePredicateTransformer()
	);

	public static void main(String[] args) throws IOException {
		var deobfuscator = new Deobfuscator(Paths.get("nonfree/code"));
		deobfuscator.run();
	}

	private final Path input;

	public Deobfuscator(Path input) {
		this.input = input;
	}

	public void run() throws IOException {
		var unpacker = Library.readJar(input.resolve("game_unpacker.dat"));
		var loader = Library.readJar(input.resolve("loader.jar"));
		var glLoader = Library.readJar(input.resolve("loader_gl.jar"));
		var client = Library.readJar(input.resolve("runescape.jar"));
		var glClient = Library.readPack(input.resolve("runescape_gl.pack200"));

		var libraries = Map.of(
			"unpacker", unpacker,
			"loader", loader,
			"loader_gl", glLoader,
			"runescape", client,
			"runescape_gl", glClient
		);

		for (var entry : libraries.entrySet()) {
			logger.info("Transforming library {}", entry.getKey());

			for (var transformer : TRANSFORMERS) {
				logger.info("Running transformer {}", transformer.getClass().getSimpleName());
				transformer.transform(entry.getValue());
			}
		}
	}
}
