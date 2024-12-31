package org.openrs2.deob.bytecode

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.JarLibraryWriter
import org.openrs2.asm.io.Pack200LibraryReader
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.bytecode.remap.ClassNamePrefixRemapper
import org.openrs2.deob.bytecode.remap.StripClassNamePrefixRemapper
import java.nio.file.Files
import java.nio.file.Path

@Singleton
public class BytecodeDeobfuscator @Inject constructor(
    @DeobfuscatorQualifier private val allTransformers: Set<Transformer>,
    private val profile: Profile,
) {
    private val allTransformersByName = allTransformers.associateBy(Transformer::name)

    public fun run(input: Path, output: Path) {
        // read list of enabled transformers and their order from the profile
        val transformers = profile.transformers.map { name ->
            allTransformersByName[name] ?: throw IllegalArgumentException("Unknown transformer $name")
        }

        // read input jars/packs
        logger.info { "Reading input jars" }

        val libraries = mutableListOf<Library>()
        val dependencies = mutableListOf<Library>()

        profile.libraries.forEach {
            val name = it.key
            val conf = it.value

            if (conf.format != null && conf.file != null) {
                // real library
                val reader = when (conf.format) {
                    "jar" -> JarLibraryReader
                    "pack200" -> Pack200LibraryReader
                    else -> throw IllegalArgumentException("Unsupported format: ${conf.format}")
                }

                libraries += Library.read(name, input.resolve(conf.file), reader)
            } else {
                // virtual library
                libraries += Library(name)
            }
        }

        profile.dependencies?.forEach {
            val name = it.key
            val conf = it.value

            val reader = when (conf.format) {
                "jar" -> JarLibraryReader
                "pack200" -> Pack200LibraryReader
                else -> throw IllegalArgumentException("Unsupported format: ${conf.format}")
            }

            dependencies += Library.read(name, input.resolve(conf.file), reader)
        }

        for (library in libraries) {
            val conf = profile.libraries[library.name]!!
            if (conf.signedMove.isNullOrEmpty() || conf.signedMove.size != 2) {
                continue
            }

            val loaderLib = libraries.find { it.name == conf.signedMove.first() }!!
            val clientLib = libraries.find { it.name == conf.signedMove.last() }!!

            SignedClassUtils.move(loaderLib, clientLib, library)
        }

        for (library in libraries) {
            val conf = profile.libraries[library.name]!!
            if (conf.move.isNullOrEmpty()) {
                continue
            }

            for (move in conf.move) {
                val other = libraries.find { it.name == move.substringBefore('!') }!!
                val clazz = move.substringAfter('!')

                library.add(other.remove(clazz)!!)
            }
        }

        /*
         * Prefix class names with the name of the library the class
         * came from (e.g. `a` => `client!a`).
         *
         * Using ! as the separator was chosen because it is not valid in Java
         * source code, so we won't expect to see it in the obfuscator's input.
         * Furthermore, if any prefixes accidentally remain unstripped, the
         * problem will be detected quickly as the deobfuscator's output will
         * not compile. It also mirrors the syntax used in JarURLConnection,
         * which has a similar purpose.
         *
         * In the early parts of the deobfuscation pipeline, this allows us to
         * disambiguate a small number of classes in the signlink which clash
         * with classes in the client.
         *
         * After name mapping has been performed, it allows us to disambiguate
         * classes across separate libraries that have been refactored and
         * given the same name.
         *
         * For example, the client and unpackclass both contain many common
         * classes (e.g. the exception wrapper, linked list/node classes,
         * bzip2/gzip decompression classes, and so on). Giving these the same
         * names across both the client and unpackclass is desirable.
         *
         * (Unfortunately we can't deduplicate the classes, as they both expose
         * different sets of fields/methods, presumably as a result of the
         * obfuscator removing unused code.)
         */
        for (library in libraries) {
            val conf = profile.libraries[library.name]!!

            if (!conf.requires.isNullOrEmpty()) {
                val requires = conf.requires.map { libraries.find { library -> library.name == it }!! }

                val remapper = ClassNamePrefixRemapper(library, *requires.toTypedArray())
                library.remap(remapper)
            } else {
                val remapper = ClassNamePrefixRemapper(library)
                library.remap(remapper)
            }
        }

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies,
            libraries
        )

        // deobfuscate
        logger.info { "Transforming" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        // strip class name prefixes
        for (library in libraries) {
            library.remap(StripClassNamePrefixRemapper)
        }

        // write output jars
        logger.info { "Writing output jars" }

        Files.createDirectories(output)

        for (library in libraries) {
            library.write(output.resolve("${library.name}.jar"), JarLibraryWriter, classPath)
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
