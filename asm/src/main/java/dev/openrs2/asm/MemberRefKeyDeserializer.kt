package dev.openrs2.asm

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer

object MemberRefKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctx: DeserializationContext): Any {
        return MemberRef.fromString(key)
    }
}