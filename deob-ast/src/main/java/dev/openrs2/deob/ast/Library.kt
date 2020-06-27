package dev.openrs2.deob.ast

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.PrettyPrinter
import com.github.javaparser.printer.PrettyPrinterConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.utils.SourceRoot
import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.deob.util.Module
import java.util.function.Function

class Library(
    val name: String,
    private val root: SourceRoot
) : Iterable<CompilationUnit> {
    private val units = mutableMapOf<String, CompilationUnit>()

    init {
        for (unit in root.compilationUnits) {
            val name = unit.primaryType.get().fullyQualifiedName.get()
            units[name] = unit
        }
    }

    operator fun get(name: String): CompilationUnit? {
        return units[name]
    }

    override fun iterator(): Iterator<CompilationUnit> {
        return units.values.iterator()
    }

    fun save() {
        logger.info { "Saving root ${root.root}" }
        root.saveAll()
    }

    companion object {
        private val logger = InlineLogger()

        private val PC_ANNOTATION_REGEX = Regex("@Pc\\(([0-9]+)\\)\\s+")

        private val printer = Function<CompilationUnit, String>(
            PrettyPrinter(
                PrettyPrinterConfiguration()
                    .setIndentType(PrettyPrinterConfiguration.IndentType.TABS_WITH_SPACE_ALIGN)
                    .setIndentSize(1)
                    .setIndentCaseInSwitch(false)
                    .setOrderImports(true)
            )::print
        ).andThen(::stripNewlineAfterPcAnnotation)

        fun parse(module: Module): Library {
            logger.info { "Parsing root ${module.sources}" }

            val solver = CombinedTypeSolver(
                ClassLoaderTypeSolver(ClassLoader.getPlatformClassLoader()),
                JavaParserTypeSolver(module.sources)
            )
            for (dependency in module.transitiveDependencies) {
                solver.add(JavaParserTypeSolver(dependency.sources))
            }

            val config = ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_11)
                .setSymbolResolver(JavaSymbolSolver(solver))

            val root = SourceRoot(module.sources, config)
            root.printer = printer

            val results = root.tryToParseParallelized()
            for (result in results) {
                require(result.isSuccessful) { result }
            }

            return Library(module.name, root)
        }

        private fun stripNewlineAfterPcAnnotation(s: String): String {
            return s.replace(PC_ANNOTATION_REGEX, "@Pc($1) ")
        }
    }
}
