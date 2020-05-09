package dev.openrs2.asm

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

object MemberRefDeserializer : StdDeserializer<MemberRef>(MemberRef::class.java) {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext): MemberRef {
        return MemberRef.fromString(ctx.readValue(parser, String::class.java))
    }
}
