package org.openrs2.crypto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

public object SymmetricKeyDeserializer : StdDeserializer<SymmetricKey>(SymmetricKey::class.java) {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): SymmetricKey {
        return SymmetricKey.fromIntArray(ctx.readValue(parser, IntArray::class.java))
    }
}
