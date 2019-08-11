package dev.openrs2.deob.ast;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import com.google.common.collect.ImmutableList;
import dev.openrs2.deob.ast.transform.AddSubTransformer;
import dev.openrs2.deob.ast.transform.ComplementTransformer;
import dev.openrs2.deob.ast.transform.Transformer;

public final class AstDeobfuscator {
	private static final ImmutableList<Transformer> TRANSFORMERS = ImmutableList.of(
		new AddSubTransformer(),
		new ComplementTransformer()
	);

	public static void main(String[] args) {
		var deobfuscator = new AstDeobfuscator(ImmutableList.of(
			Paths.get("nonfree/client/src/main/java"),
			Paths.get("nonfree/gl/src/main/java"),
			Paths.get("nonfree/gl-dri/src/main/java"),
			Paths.get("nonfree/loader/src/main/java"),
			Paths.get("nonfree/signlink/src/main/java"),
			Paths.get("nonfree/unpack/src/main/java"),
			Paths.get("nonfree/unpacker/src/main/java")
		));
		deobfuscator.run();
	}

	private final ImmutableList<Path> modules;

	public AstDeobfuscator(ImmutableList<Path> modules) {
		this.modules = modules;
	}

	public void run() {
		var solver = new CombinedTypeSolver(new ReflectionTypeSolver(true));
		for (var module : modules) {
			solver.add(new JavaParserTypeSolver(module));
		}

		var config = new ParserConfiguration()
			.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8)
			.setSymbolResolver(new JavaSymbolSolver(solver));

		var printerConfig = new PrettyPrinterConfiguration()
			.setIndentType(PrettyPrinterConfiguration.IndentType.TABS_WITH_SPACE_ALIGN)
			.setIndentSize(1);

		var printer = new PrettyPrinter(printerConfig);

		for (var module : modules) {
			var root = new SourceRoot(module, config);

			var results = root.tryToParseParallelized();
			for (var result : results) {
				if (!result.isSuccessful()) {
					throw new IllegalArgumentException(result.toString());
				}
			}

			root.getCompilationUnits().forEach(unit -> {
				TRANSFORMERS.forEach(transformer -> {
					transformer.transform(unit);
				});
			});

			root.setPrinter(printer::print);
			root.saveAll();
		}
	}
}
