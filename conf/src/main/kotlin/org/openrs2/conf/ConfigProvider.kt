package org.openrs2.conf

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

public class ConfigProvider @Inject constructor(@Yaml private val mapper: ObjectMapper) : Provider<Config> {
    override fun get(): Config {
        if (Files.notExists(CONFIG_PATH)) {
            Files.copy(EXAMPLE_CONFIG_PATH, CONFIG_PATH)
        }

        return Files.newBufferedReader(CONFIG_PATH).use { reader ->
            mapper.readValue(reader, Config::class.java)
        }
    }

    private companion object {
        private val CONFIG_PATH = Path.of("etc/config.yaml")
        private val EXAMPLE_CONFIG_PATH = Path.of("etc/config.example.yaml")
    }
}
