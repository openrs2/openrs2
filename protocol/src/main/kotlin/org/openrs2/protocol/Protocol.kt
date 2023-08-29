package org.openrs2.protocol

import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class Protocol @Inject constructor(codecs: Set<PacketCodec<*>>) {
    public constructor(vararg codecs: PacketCodec<*>) : this(codecs.toSet())

    private val decoders = arrayOfNulls<PacketCodec<*>>(256)
    private val encoders = codecs.associateBy(PacketCodec<*>::type)

    init {
        for (codec in codecs) {
            decoders[codec.opcode] = codec
        }
    }

    public fun getDecoder(opcode: Int): PacketCodec<*>? {
        require(opcode in decoders.indices)
        return decoders[opcode]
    }

    @Suppress("UNCHECKED_CAST")
    public fun <T : Packet> getEncoder(type: Class<T>): PacketCodec<T>? {
        return encoders[type] as PacketCodec<T>?
    }
}
