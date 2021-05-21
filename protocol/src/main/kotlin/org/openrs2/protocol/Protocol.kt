package org.openrs2.protocol

import org.openrs2.protocol.login.ClientOutOfDateCodec
import org.openrs2.protocol.login.InitJaggrabConnectionCodec
import org.openrs2.protocol.login.InitJs5RemoteConnectionCodec
import org.openrs2.protocol.login.IpLimitCodec
import org.openrs2.protocol.login.Js5OkCodec
import org.openrs2.protocol.login.RequestWorldListCodec
import org.openrs2.protocol.login.ServerFullCodec

public class Protocol(vararg codecs: PacketCodec<*>) {
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

    public companion object {
        public val LOGIN_UPSTREAM: Protocol = Protocol(
            InitJs5RemoteConnectionCodec,
            InitJaggrabConnectionCodec,
            RequestWorldListCodec
        )
        public val LOGIN_DOWNSTREAM: Protocol = Protocol(
            Js5OkCodec,
            ClientOutOfDateCodec,
            ServerFullCodec,
            IpLimitCodec
        )
    }
}
