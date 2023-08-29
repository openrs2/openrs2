package org.openrs2.crypto

import com.fasterxml.jackson.databind.module.SimpleModule
import jakarta.inject.Singleton

@Singleton
public class CryptoJacksonModule : SimpleModule() {
    init {
        addDeserializer(XteaKey::class.java, XteaKeyDeserializer)
        addSerializer(XteaKey::class.java, XteaKeySerializer)
    }
}
