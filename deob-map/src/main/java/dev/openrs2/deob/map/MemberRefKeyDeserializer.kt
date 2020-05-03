package dev.openrs2.deob.map

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import dev.openrs2.asm.MemberRef

object MemberRefKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctx: DeserializationContext): Any {
        return MemberRef.fromString(key)
    }
}
