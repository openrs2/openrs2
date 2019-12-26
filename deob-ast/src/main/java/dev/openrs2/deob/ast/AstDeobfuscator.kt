package dev.openrs2.deob.ast

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.printer.PrettyPrinter
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import dev.openrs2.deob.ast.transform.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function

class AstDeobfuscator(private val modules: List<Path>) {
    fun run() {
        val solver = CombinedTypeSolver(ReflectionTypeSolver(true))
        for (module in modules) {
            solver.add(JavaParserTypeSolver(module))
        }

        val config = ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8)
            .setSymbolResolver(JavaSymbolSolver(solver))

        val printerConfig = PrettyPrinterConfiguration()
            .setIndentType(PrettyPrinterConfiguration.IndentType.TABS_WITH_SPACE_ALIGN)
            .setIndentSize(1)

        val printer = PrettyPrinter(printerConfig)

        for (module in modules) {
            val root = SourceRoot(module, config)

            val results = root.tryToParseParallelized()
            for (result in results) {
                require(result.isSuccessful) { result }
            }

            root.compilationUnits.forEach { unit ->
                TRANSFORMERS.forEach { transformer ->
                    transformer.transform(unit)
                }
            }

            root.printer = Function(printer::print)
            root.saveAll()
        }
    }

    companion object {
        private val TRANSFORMERS = listOf(
            UnencloseTransformer(),
            NegativeLiteralTransformer(),
            ComplementTransformer(),
            IfElseTransformer(),
            TernaryTransformer(),
            BinaryExprOrderTransformer(),
            AddSubTransformer(),
            BitMaskTransformer(),
            ValueOfTransformer(),
            NewInstanceTransformer(),
            EncloseTransformer()
        )

        @JvmStatic
        fun main(args: Array<String>) {
            val deobfuscator = AstDeobfuscator(
                listOf(
                    Paths.get("nonfree/client/src/main/java"),
                    Paths.get("nonfree/gl/src/main/java"),
                    Paths.get("nonfree/gl-dri/src/main/java"),
                    Paths.get("nonfree/loader/src/main/java"),
                    Paths.get("nonfree/signlink/src/main/java"),
                    Paths.get("nonfree/unpack/src/main/java"),
                    Paths.get("nonfree/unpacker/src/main/java")
                )
            )
            deobfuscator.run()
        }
    }
}
