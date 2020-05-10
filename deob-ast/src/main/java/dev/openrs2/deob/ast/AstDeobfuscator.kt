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
import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.deob.ast.transform.AddSubTransformer
import dev.openrs2.deob.ast.transform.BinaryExprOrderTransformer
import dev.openrs2.deob.ast.transform.BitMaskTransformer
import dev.openrs2.deob.ast.transform.ComplementTransformer
import dev.openrs2.deob.ast.transform.EncloseTransformer
import dev.openrs2.deob.ast.transform.ForLoopConditionTransformer
import dev.openrs2.deob.ast.transform.GlTransformer
import dev.openrs2.deob.ast.transform.IdentityTransformer
import dev.openrs2.deob.ast.transform.IfElseTransformer
import dev.openrs2.deob.ast.transform.IncrementTransformer
import dev.openrs2.deob.ast.transform.NegativeLiteralTransformer
import dev.openrs2.deob.ast.transform.NewInstanceTransformer
import dev.openrs2.deob.ast.transform.TernaryTransformer
import dev.openrs2.deob.ast.transform.UnencloseTransformer
import dev.openrs2.deob.ast.transform.ValueOfTransformer
import java.nio.file.Path
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
            .setIndentCaseInSwitch(false)
            .setOrderImports(true)

        val printer = PrettyPrinter(printerConfig)

        val roots = modules.map { SourceRoot(it, config) }
        val units = mutableMapOf<String, CompilationUnit>()

        for (root in roots) {
            logger.info { "Parsing root ${root.root}" }
            val results = root.tryToParseParallelized()
            for (result in results) {
                require(result.isSuccessful) { result }
            }

            for (unit in root.compilationUnits) {
                val name = unit.primaryType.get().fullyQualifiedName.get()
                units[name] = unit
            }
        }

        for (transformer in TRANSFORMERS) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(units)
        }

        for (root in roots) {
            logger.info { "Saving root ${root.root}" }
            root.printer = Function<CompilationUnit, String>(printer::print).andThen(::stripNewlineAfterPcAnnotation)
            root.saveAll()
        }
    }

    private companion object {
        private val logger = InlineLogger()

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
            GlTransformer(),
            EncloseTransformer()
        )
        private val PC_ANNOTATION_REGEX = Regex("@Pc\\(([0-9]+)\\)\\s+")

        private fun stripNewlineAfterPcAnnotation(s: String): String {
            return s.replace(PC_ANNOTATION_REGEX, "@Pc($1) ")
        }
    }
}
