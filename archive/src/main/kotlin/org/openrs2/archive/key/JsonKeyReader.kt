package org.openrs2.archive.key

import com.fasterxml.jackson.databind.ObjectMapper
import org.openrs2.crypto.XteaKey
import org.openrs2.json.Json
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class JsonKeyReader @Inject constructor(
    @Json private val mapper: ObjectMapper
) : KeyReader {
    override fun read(input: InputStream): Sequence<XteaKey> {
        return sequence {
            for (mapSquare in mapper.readTree(input)) {
                val key = mapSquare["key"] ?: mapSquare["keys"] ?: continue

                if (key.size() != 4) {
                    continue
                }

                val k0 = key[0].asText().toIntOrNull() ?: continue
                val k1 = key[1].asText().toIntOrNull() ?: continue
                val k2 = key[2].asText().toIntOrNull() ?: continue
                val k3 = key[3].asText().toIntOrNull() ?: continue

                yield(XteaKey(k0, k1, k2, k3))
            }
        }
    }
}
