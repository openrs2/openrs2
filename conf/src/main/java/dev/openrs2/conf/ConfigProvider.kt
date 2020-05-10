package dev.openrs2.conf

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

class ConfigProvider @Inject constructor(private val mapper: ObjectMapper) : Provider<Config> {
    override fun get(): Config {
        if (Files.notExists(CONFIG_PATH)) {
            Files.copy(EXAMPLE_CONFIG_PATH, CONFIG_PATH)
        }

        return Files.newBufferedReader(CONFIG_PATH).use { reader ->
            mapper.readValue(reader, Config::class.java)
        }
    }

    private companion object {
        private val CONFIG_PATH = Paths.get("etc/config.yaml")
        private val EXAMPLE_CONFIG_PATH = Paths.get("etc/config.example.yaml")
    }
}
