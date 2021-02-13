package org.openrs2.crypto

import com.fasterxml.jackson.databind.module.SimpleModule
import javax.inject.Singleton

@Singleton
public class CryptoJacksonModule : SimpleModule() {
    init {
        addDeserializer(XteaKey::class.java, XteaKeyDeserializer)
        addSerializer(XteaKey::class.java, XteaKeySerializer)
    }
}
