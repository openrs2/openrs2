package org.openrs2.crypto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

public object XteaKeyDeserializer : StdDeserializer<XteaKey>(XteaKey::class.java) {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): XteaKey {
        return XteaKey.fromIntArray(ctx.readValue(parser, IntArray::class.java))
    }
}
