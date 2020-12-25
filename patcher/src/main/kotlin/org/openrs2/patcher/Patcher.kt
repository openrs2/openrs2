package org.openrs2.patcher

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.io.JarLibraryReader
import org.openrs2.asm.io.JarLibraryWriter
import org.openrs2.asm.io.ManifestJarLibraryWriter
import org.openrs2.asm.io.Pack200LibraryReader
import org.openrs2.asm.io.Pack200LibraryWriter
import org.openrs2.asm.io.PackClassLibraryWriter
import org.openrs2.asm.io.SignedJarLibraryWriter
import org.openrs2.asm.transform.Transformer
import org.openrs2.conf.Config
import org.openrs2.crypto.Pkcs12KeyStore
import org.openrs2.patcher.transform.ResourceTransformer
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.Attributes.Name.MANIFEST_VERSION
import java.util.jar.Manifest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class Patcher @Inject constructor(
    @PatcherQualifier private val transformers: Set<Transformer>,
    private val config: Config
) {
    private val unsignedManifest = Manifest().apply {
        mainAttributes[MANIFEST_VERSION] = "1.0"
        mainAttributes[APPLICATION_NAME] = config.game
        mainAttributes[PERMISSIONS] = "sandbox"
    }

    private val signedManifest = Manifest().apply {
        mainAttributes[MANIFEST_VERSION] = "1.0"
        mainAttributes[APPLICATION_NAME] = config.game
        mainAttributes[PERMISSIONS] = "all-permissions"
    }

    public fun run(input: Path, output: Path, keyStorePath: Path) {
        // read input jars/packs
        logger.info { "Reading input jars" }
        val unpacker = Library.read("unpackclass", input.resolve("unpackclass.pack"), JarLibraryReader)
        val loader = Library.read("loader_sd", input.resolve("loader.jar"), JarLibraryReader)
        val glLoader = Library.read("loader_hd", input.resolve("loader_gl.jar"), JarLibraryReader)
        val gl = Library.read("gl", input.resolve("jaggl.pack200"), Pack200LibraryReader)
        val client = Library.read("client_sd", input.resolve("runescape.jar"), JarLibraryReader)
        val glClient = Library.read("client_hd", input.resolve("runescape_gl.pack200"), Pack200LibraryReader)

        // bundle libraries together into a common classpath
        val runtime = ClassLoader.getPlatformClassLoader()
        val classPath = ClassPath(
            runtime,
            dependencies = listOf(unpacker),
            libraries = listOf(client, loader)
        )
        val glClassPath = ClassPath(
            runtime,
            dependencies = listOf(gl, unpacker),
            libraries = listOf(glClient, glLoader)
        )

        // run simple transformers
        logger.info { "Transforming client" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }

        logger.info { "Transforming client_gl" }
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(glClassPath)
        }

        // compress resources
        logger.info { "Compressing resources" }

        val unpackerJar = Resource.compressLibrary(
            "unpackclass.pack", "game_unpacker.dat", classPath, unpacker, JarLibraryWriter
        )
        val clientPack = Resource.compressLibrary(
            "runescape.pack200", "main_file_cache.dat0", classPath, client, Pack200LibraryWriter
        )
        val clientJs5 = Resource.compressLibrary(
            "runescape.js5", "main_file_cache.dat1", classPath, client, PackClassLibraryWriter
        )
        val glClientPack = Resource.compressLibrary(
            "runescape_gl.pack200", "main_file_cache.dat3", glClassPath, glClient, Pack200LibraryWriter
        )
        val glClientJs5 = Resource.compressLibrary(
            "runescape_gl.js5", "main_file_cache.dat4", glClassPath, glClient, PackClassLibraryWriter
        )
        val glPack = Resource.compressLibrary(
            "jaggl.pack200", "main_file_cache.dat5", glClassPath, gl, Pack200LibraryWriter
        )
        val glJs5 = Resource.compressLibrary(
            "jaggl.js5", "main_file_cache.dat6", glClassPath, gl, PackClassLibraryWriter
        )

        val glNatives = Resource.compressGlNatives()
        val miscNatives = Resource.compressMiscNatives()

        // update checksums in the loader
        logger.info { "Updating checksums" }

        val resourceTransformer = ResourceTransformer(
            resources = listOf(unpackerJar, clientPack, clientJs5),
            glResources = glNatives,
            miscResources = miscNatives
        )
        resourceTransformer.transform(classPath)

        val glResourceTransformer = ResourceTransformer(
            resources = listOf(unpackerJar, glClientPack, glClientJs5, glPack, glJs5),
            glResources = glNatives,
            miscResources = miscNatives
        )
        glResourceTransformer.transform(glClassPath)

        // write all resources to disk
        logger.info { "Writing resources" }

        Files.createDirectories(output)

        val resources = listOf(
            unpackerJar,
            clientPack,
            clientJs5,
            glClientPack,
            glClientJs5,
            glPack,
            glJs5
        ) + glNatives.flatten() + miscNatives
        for (resource in resources) {
            resource.write(output)
        }

        // write unsigned client and loaders
        client.write(output.resolve("runescape.jar"), ManifestJarLibraryWriter(unsignedManifest), classPath)

        val keyStore = Pkcs12KeyStore.open(keyStorePath, config.game)
        loader.write(output.resolve("loader.jar"), SignedJarLibraryWriter(signedManifest, keyStore), classPath)
        glLoader.write(output.resolve("loader_gl.jar"), SignedJarLibraryWriter(signedManifest, keyStore), glClassPath)
    }

    private companion object {
        private val logger = InlineLogger()

        private val APPLICATION_NAME = Attributes.Name("Application-Name")
        private val PERMISSIONS = Attributes.Name("Permissions")
    }
}
