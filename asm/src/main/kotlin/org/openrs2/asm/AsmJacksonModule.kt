package org.openrs2.asm

import com.fasterxml.jackson.databind.module.SimpleModule
import jakarta.inject.Singleton

@Singleton
public class AsmJacksonModule : SimpleModule() {
    init {
        addDeserializer(MemberRef::class.java, MemberRefDeserializer)
        addKeyDeserializer(MemberRef::class.java, MemberRefKeyDeserializer)
    }
}
