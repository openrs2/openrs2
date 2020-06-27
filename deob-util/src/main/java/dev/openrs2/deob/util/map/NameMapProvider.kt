package dev.openrs2.deob.util.map

import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Provider

class NameMapProvider @Inject constructor(private val mapper: ObjectMapper) : Provider<NameMap> {
    override fun get(): NameMap {
        val combinedMap = NameMap()

        if (!Files.exists(PATH)) {
            return combinedMap
        }

        for (file in Files.list(PATH).filter(::isYamlFile)) {
            val map = Files.newBufferedReader(file).use { reader ->
                mapper.readValue(reader, NameMap::class.java)
            }
            combinedMap.add(map)
        }

        return combinedMap
    }

    private fun isYamlFile(path: Path): Boolean {
        return Files.isRegularFile(path) && path.fileName.toString().endsWith(YAML_SUFFIX)
    }

    private companion object {
        private val PATH = Paths.get("share/deob/map")
        private const val YAML_SUFFIX = ".yaml"
    }
}
