package org.openrs2.archive

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.yaml.Yaml
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Path
import kotlin.io.path.bufferedReader
import kotlin.io.path.copyTo
import kotlin.io.path.notExists
import kotlin.io.path.toPath

public class ArchiveConfigProvider @Inject constructor(
    @param:Yaml private val mapper: ObjectMapper
) : Provider<ArchiveConfig> {
    override fun get(): ArchiveConfig {
        return getConfigPath().bufferedReader().use { reader ->
            mapper.readValue(reader, ArchiveConfig::class.java)
        }
    }

    private fun getRoot(): Path {
        // find the location of openrs2.jar
        val codeSource = ArchiveConfigProvider::class.java.protectionDomain.codeSource
        if (codeSource != null) {
            val path = try {
                codeSource.location.toURI().toPath()
            } catch (_: FileSystemNotFoundException) {
                null
            }

            // go from <root>/lib/openrs2.jar to <root>
            val root = path?.parent?.parent
            if (root != null) {
                return root
            }
        }

        // fall back to current working directory
        return Path.of("")
    }

    private fun getConfigPath(): Path {
        val root = getRoot()
        val path = root.resolve(CONFIG_PATH)

        if (path.notExists()) {
            root.resolve(EXAMPLE_CONFIG_PATH).copyTo(path)
        }

        return path
    }

    private companion object {
        private val CONFIG_PATH = Path.of("etc/archive.yaml")
        private val EXAMPLE_CONFIG_PATH = Path.of("etc/archive.example.yaml")
    }
}
