package org.openrs2.deob.bytecode

import com.github.michaelbull.logging.InlineLogger
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class BytecodeDeobfuscator @Inject constructor(
    @DeobfuscatorQualifier private val transformers: Set<Transformer>
) {
    public fun run(input: Path, output: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val client = Library.read("client", input.resolve("runescape_gl.pack200"), Pack200LibraryReader)
        val gl = Library.read("gl", input.resolve("jaggl.pack200"), Pack200LibraryReader)
        val loader = Library.read("loader", input.resolve("loader_gl.jar"), JarLibraryReader)
        val unpackClass = Library.read("unpackclass", input.resolve("unpackclass.pack"), JarLibraryReader)

        // overwrite client's classes with signed classes from the loader
        logger.info { "Moving signed classes from loader to signlink" }
        val signLink = Library("signlink")
        SignedClassUtils.move(loader, client, signLink)

        // move unpack class out of the loader (so the unpacker and loader can both depend on it)
        logger.info { "Moving unpack from loader to unpack" }
        val unpack = Library("unpack")
        unpack.add(loader.remove("unpack")!!)

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
        val clientRemapper = ClassNamePrefixRemapper(client, gl, signLink)
        val glRemapper = ClassNamePrefixRemapper(gl)
        val loaderRemapper = ClassNamePrefixRemapper(loader, signLink, unpack)
        val signLinkRemapper = ClassNamePrefixRemapper(signLink)
        val unpackClassRemapper = ClassNamePrefixRemapper(unpackClass, unpack)
        val unpackRemapper = ClassNamePrefixRemapper(unpack)

        client.remap(clientRemapper)
        gl.remap(glRemapper)
        loader.remap(loaderRemapper)
        signLink.remap(signLinkRemapper)
        unpack.remap(unpackRemapper)
        unpackClass.remap(unpackClassRemapper)

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = emptyList(),
            libraries = listOf(client, gl, loader, signLink, unpack, unpackClass)
        )

        // deobfuscate
        logger.info { "Transforming client" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        // strip class name prefixes
        client.remap(StripClassNamePrefixRemapper)
        gl.remap(StripClassNamePrefixRemapper)
        loader.remap(StripClassNamePrefixRemapper)
        signLink.remap(StripClassNamePrefixRemapper)
        unpack.remap(StripClassNamePrefixRemapper)
        unpackClass.remap(StripClassNamePrefixRemapper)

        // write output jars
        logger.info { "Writing output jars" }

        Files.createDirectories(output)

        client.write(output.resolve("client.jar"), JarLibraryWriter, classPath)
        gl.write(output.resolve("gl.jar"), JarLibraryWriter, classPath)
        loader.write(output.resolve("loader.jar"), JarLibraryWriter, classPath)
        signLink.write(output.resolve("signlink.jar"), JarLibraryWriter, classPath)
        unpack.write(output.resolve("unpack.jar"), JarLibraryWriter, classPath)
        unpackClass.write(output.resolve("unpackclass.jar"), JarLibraryWriter, classPath)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
