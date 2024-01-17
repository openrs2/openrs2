package org.openrs2.deob.ast

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.DefaultPrettyPrinter
import com.github.javaparser.printer.configuration.DefaultConfigurationOption
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.INDENTATION
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.INDENT_CASE_IN_SWITCH
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption.ORDER_IMPORTS
import com.github.javaparser.printer.configuration.Indentation
import com.github.javaparser.printer.configuration.Indentation.IndentType.TABS_WITH_SPACE_ALIGN
import com.github.javaparser.resolution.TypeSolver
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.utils.SourceRoot
import com.github.michaelbull.logging.InlineLogger
import org.openrs2.deob.util.Module
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function

public class Library(
    public val name: String,
    private val root: SourceRoot
) : Iterable<CompilationUnit> {
    private val units = mutableMapOf<String, CompilationUnit>()

    init {
        for (unit in root.compilationUnits) {
            val name = unit.primaryType.get().fullyQualifiedName.get()
            units[name] = unit
        }
    }

    public operator fun get(name: String): CompilationUnit? {
        return units[name]
    }

    override fun iterator(): Iterator<CompilationUnit> {
        return units.values.iterator()
    }

    public fun save() {
        logger.info { "Saving root ${root.root}" }
        root.saveAll()
    }

    public companion object {
        private val logger = InlineLogger()

        private val PC_ANNOTATION_REGEX = Regex("@Pc\\(([0-9]+)\\)\\s+")

        private val printer = Function<CompilationUnit, String>(
            DefaultPrettyPrinter(
                DefaultPrinterConfiguration()
                    .addOption(DefaultConfigurationOption(INDENTATION, Indentation(TABS_WITH_SPACE_ALIGN, 1)))
                    .addOption(DefaultConfigurationOption(INDENT_CASE_IN_SWITCH, false))
                    .addOption(DefaultConfigurationOption(ORDER_IMPORTS, true))
            )::print
        ).andThen(::stripNewlineAfterPcAnnotation)

        public fun parse(module: Module): Library {
            logger.info { "Parsing root ${module.sources}" }

            val extraSolvers: MutableList<TypeSolver> = mutableListOf()
            if (Files.exists(Paths.get("share", "deob", "jars.dependencies.list"))) {
                // would be nice to read this from yaml, unfortunately profile.yaml is not available here
                val files = Files.readAllLines(Paths.get("share", "deob", "jars.dependencies.list"))

                for (file in files) {
                    if (file.isEmpty()) {
                        continue
                    }

                    extraSolvers += JarTypeSolver(file)
                }
            }

            val solver = CombinedTypeSolver(
                ClassLoaderTypeSolver(ClassLoader.getPlatformClassLoader()),
                *extraSolvers.toTypedArray(),
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
