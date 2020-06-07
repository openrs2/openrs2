package dev.openrs2.deob

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.io.JarLibraryReader
import dev.openrs2.asm.io.JarLibraryWriter
import dev.openrs2.asm.io.Pack200LibraryReader
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.PrefixRemapper
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Deobfuscator @Inject constructor(
    private val profile: Profile,
    @DeobfuscatorQualifier private val transformers: Set<@JvmSuppressWildcards Transformer>
) {
    fun run(input: Path, output: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val client = Library.read(input.resolve("runescape_gl.pack200"), Pack200LibraryReader)
        val gl = Library.read(input.resolve("jaggl.pack200"), Pack200LibraryReader)
        val loader = Library.read(input.resolve("loader_gl.jar"), JarLibraryReader)
        val unpackClass = Library.read(input.resolve("unpackclass.pack"), JarLibraryReader)

        // overwrite client's classes with signed classes from the loader
        logger.info { "Moving signed classes from loader to signlink" }
        val signlink = Library()
        SignedClassUtils.move(loader, client, signlink)

        // move unpack class out of the loader (so the unpacker and loader can both depend on it)
        logger.info { "Moving unpack from loader to unpack" }
        val unpack = Library()
        unpack.add(loader.remove("unpack")!!)

        // prefix remaining loader/unpacker classes (to avoid conflicts when we rename in the same classpath as the client)
        logger.info { "Prefixing loader and unpackclass class names" }
        loader.remap(PrefixRemapper.create(loader, "loader_", profile.excludedClasses))
        unpackClass.remap(PrefixRemapper.create(unpackClass, "unpackclass_", profile.excludedClasses))

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = emptyList(),
            libraries = listOf(client, gl, loader, signlink, unpack, unpackClass)
        )

        // deobfuscate
        logger.info { "Transforming client" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        // write output jars
        logger.info { "Writing output jars" }

        Files.createDirectories(output)

        client.write(output.resolve("runescape_gl.jar"), JarLibraryWriter, classPath)
        gl.write(output.resolve("jaggl.jar"), JarLibraryWriter, classPath)
        loader.write(output.resolve("loader_gl.jar"), JarLibraryWriter, classPath)
        signlink.write(output.resolve("signlink_gl.jar"), JarLibraryWriter, classPath)
        unpack.write(output.resolve("unpack_gl.jar"), JarLibraryWriter, classPath)
        unpackClass.write(output.resolve("unpackclass_gl.jar"), JarLibraryWriter, classPath)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
