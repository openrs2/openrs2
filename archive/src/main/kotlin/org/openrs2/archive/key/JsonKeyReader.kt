package org.openrs2.archive.key

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.crypto.XteaKey
import org.openrs2.json.Json
import java.io.IOException
import java.io.InputStream

@Singleton
public class JsonKeyReader @Inject constructor(
    @Json private val mapper: ObjectMapper
) : KeyReader {
    override fun read(input: InputStream): Sequence<XteaKey> {
        val keys = mutableSetOf<XteaKey>()
        val root = mapper.readTree(input)

        when {
            root.isArray -> {
                for (entry in root) {
                    val key = entry["key"] ?: entry["keys"] ?: throw IOException("Missing 'key' or 'keys' field")
                    keys += mapper.treeToValue<XteaKey?>(key) ?: throw IOException("Key must be non-null")
                }
            }

            root.isObject -> {
                for (entry in root.fields()) {
                    keys += mapper.treeToValue<XteaKey?>(entry.value) ?: throw IOException("Key must be non-null")
                }
            }

            else -> throw IOException("Root element must be an array or object")
        }

        return keys.asSequence()
    }
}
