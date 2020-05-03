package dev.openrs2.deob.map

import com.fasterxml.jackson.databind.module.SimpleModule
import dev.openrs2.asm.MemberRef
import javax.inject.Singleton

@Singleton
class DeobfuscatorMapJacksonModule : SimpleModule() {
    init {
        addKeyDeserializer(MemberRef::class.java, MemberRefKeyDeserializer)
    }
}
