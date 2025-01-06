package org.openrs2.deob.bytecode

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.io.JarLibraryWriter
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.bytecode.remap.ClassNamePrefixRemapper
import org.openrs2.deob.bytecode.remap.StripClassNamePrefixRemapper
import org.openrs2.deob.util.module.Module
import org.openrs2.deob.util.module.ModuleType
import org.openrs2.deob.util.profile.Profile
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumMap

@Singleton
public class BytecodeDeobfuscator @Inject constructor(
    @param:DeobfuscatorQualifier private val allTransformers: Set<Transformer>,
    private val profile: Profile,
    modules: Set<Module>,
) {
    private val allTransformersByName = allTransformers.associateBy(Transformer::name)
    private val modules = modules.associateByTo(EnumMap(ModuleType::class.java), Module::type)

    public fun run() {
        val input = profile.directory.resolve("lib")
        logger.info { "Reading input jars from $input" }
        val libraries = modules.mapValuesTo(EnumMap(ModuleType::class.java)) { (_, module) -> module.toLibrary(input) }

        if (ModuleType.LOADER in modules) {
            val loader = libraries[ModuleType.LOADER]!!

            // overwrite client's classes with signed classes from the loader
            if (ModuleType.CLIENT in modules && ModuleType.SIGNLINK in modules) {
                logger.info { "Moving signed classes from loader to signlink" }
                SignedClassUtils.move(loader, libraries[ModuleType.CLIENT]!!, libraries[ModuleType.SIGNLINK]!!)
            }

            // move unpack class out of the loader (so the unpacker and loader can both depend on it)
            if (ModuleType.UNPACK in modules) {
                logger.info { "Moving unpack from loader to unpack" }
                libraries[ModuleType.UNPACK]!!.add(loader.remove("unpack")!!)
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

        // create all remappers prior to remapping, otherwise the later ones will build the wrong mapping table
        libraries.map { (type, library) ->
            val libs = modules[type]!!.transitiveDependencies.mapTo(mutableListOf(library)) { libraries[it.type]!! }
            library to ClassNamePrefixRemapper(libs)
        }
            .forEach { (library, remapper) -> library.remap(remapper) }

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = emptyList(),
            libraries = libraries.values.toList()
        )

        // read list of enabled transformers and their order from the profile
        val transformers = profile.transformers.map { name ->
            requireNotNull(allTransformersByName[name]) { "Unknown transformer $name" }
        }

        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        libraries.values.forEach { it.remap(StripClassNamePrefixRemapper) }

        logger.info { "Writing output jars" }
        for ((type, library) in libraries) {
            val path = modules[type]!!.jar
            Files.createDirectories(path.parent)

            library.write(path, JarLibraryWriter, classPath)
        }
    }

    private fun Module.toLibrary(directory: Path): Library {
        return if (type.synthetic) {
            Library(name)
        } else {
            Library.read(name, directory.resolve(source), format.reader)
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
