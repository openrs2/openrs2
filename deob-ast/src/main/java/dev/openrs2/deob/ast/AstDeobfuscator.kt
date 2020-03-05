package dev.openrs2.deob.ast

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.PrettyPrinter
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import com.github.javaparser.utils.SourceRoot
import dev.openrs2.deob.ast.transform.AddSubTransformer
import dev.openrs2.deob.ast.transform.BinaryExprOrderTransformer
import dev.openrs2.deob.ast.transform.BitMaskTransformer
import dev.openrs2.deob.ast.transform.ComplementTransformer
import dev.openrs2.deob.ast.transform.EncloseTransformer
import dev.openrs2.deob.ast.transform.ForLoopConditionTransformer
import dev.openrs2.deob.ast.transform.IdentityTransformer
import dev.openrs2.deob.ast.transform.IfElseTransformer
import dev.openrs2.deob.ast.transform.IncrementTransformer
import dev.openrs2.deob.ast.transform.NegativeLiteralTransformer
import dev.openrs2.deob.ast.transform.NewInstanceTransformer
import dev.openrs2.deob.ast.transform.TernaryTransformer
import dev.openrs2.deob.ast.transform.UnencloseTransformer
import dev.openrs2.deob.ast.transform.ValueOfTransformer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function

fun main() {
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

        val roots = modules.map { SourceRoot(it, config) }
        val units = mutableMapOf<String, CompilationUnit>()

        for (root in roots) {
            val results = root.tryToParseParallelized()
            for (result in results) {
                require(result.isSuccessful) { result }
            }

            for (unit in root.compilationUnits) {
                val name = unit.primaryType.orElseThrow().fullyQualifiedName.orElseThrow()
                units[name] = unit
            }
        }

        for (transformer in TRANSFORMERS) {
            transformer.transform(units)
        }

        for (root in roots) {
            root.printer = Function<CompilationUnit, String>(printer::print)
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
            IdentityTransformer(),
            BitMaskTransformer(),
            ValueOfTransformer(),
            NewInstanceTransformer(),
            IncrementTransformer(),
            ForLoopConditionTransformer(),
            EncloseTransformer()
        )
    }
}
