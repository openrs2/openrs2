package dev.openrs2.decompiler;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public final class Decompiler implements Closeable {
	private static final Map<String, Object> OPTIONS = Map.of(
		IFernflowerPreferences.ASCII_STRING_CHARACTERS, "1",
		IFernflowerPreferences.INDENT_STRING, "\t",
		IFernflowerPreferences.SYNTHETIC_NOT_SET, "1"
	);

	private static Path getDestination(String archive) {
		archive = archive.replaceAll("(?:_gl)?[.]jar$", "");
		if (archive.equals("runescape")) {
			archive = "client";
		} else if (archive.equals("jaggl")) {
			archive = "gl";
		} else if (archive.equals("jaggl_dri")) {
			archive = "gl-dri";
		}
		return Paths.get("nonfree").resolve(archive).resolve("src/main/java");
	}

	public static void main(String[] args) throws IOException {
		var deobOutput = Paths.get("nonfree/code/deob");
		var sources = List.of(
			deobOutput.resolve("runescape_gl.jar"),
			deobOutput.resolve("jaggl.jar"),
			deobOutput.resolve("jaggl_dri.jar"),
			deobOutput.resolve("loader_gl.jar"),
			deobOutput.resolve("signlink_gl.jar"),
			deobOutput.resolve("unpack_gl.jar"),
			deobOutput.resolve("unpacker_gl.jar")
		);

		try (var decompiler = new Decompiler(sources, Decompiler::getDestination)) {
			decompiler.run();
		}
	}

	private final DecompilerIo io;
	private final Fernflower fernflower;
	private final List<Path> sources;

	public Decompiler(List<Path> sources, Function<String, Path> destination) {
		this.io = new DecompilerIo(destination);
		this.fernflower = new Fernflower(io, io, OPTIONS, Slf4jFernflowerLogger.INSTANCE);
		this.sources = sources;
	}

	public void run() {
		for (var source : sources) {
			fernflower.addSource(source.toFile());
		}

		fernflower.decompileContext();
	}

	@Override
	public void close() throws IOException {
		io.close();
	}
}
