package org.openrs2.archive

import com.fasterxml.jackson.databind.ObjectMapper
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Provider

public class ArchiveConfigProvider @Inject constructor(
    @Yaml private val mapper: ObjectMapper
) : Provider<ArchiveConfig> {
    override fun get(): ArchiveConfig {
        if (Files.notExists(CONFIG_PATH)) {
            Files.copy(EXAMPLE_CONFIG_PATH, CONFIG_PATH)
        }

        return Files.newBufferedReader(CONFIG_PATH).use { reader ->
            mapper.readValue(reader, ArchiveConfig::class.java)
        }
    }

    private companion object {
        private val CONFIG_PATH = Path.of("etc/archive.yaml")
        private val EXAMPLE_CONFIG_PATH = Path.of("etc/archive.example.yaml")
    }
}
