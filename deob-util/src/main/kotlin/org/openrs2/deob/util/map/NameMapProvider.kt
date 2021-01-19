package org.openrs2.deob.util.map

import com.fasterxml.jackson.databind.ObjectMapper
import org.openrs2.yaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Provider

public class NameMapProvider @Inject constructor(@Yaml private val mapper: ObjectMapper) : Provider<NameMap> {
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
        private val PATH = Path.of("share/deob/map")
        private const val YAML_SUFFIX = ".yaml"
    }
}
