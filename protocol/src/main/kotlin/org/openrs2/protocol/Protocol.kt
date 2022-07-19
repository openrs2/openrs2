package org.openrs2.protocol

import org.openrs2.protocol.login.ClientOutOfDateCodec
import org.openrs2.protocol.login.InitCrossDomainConnectionCodec
import org.openrs2.protocol.login.InitGameConnectionCodec
import org.openrs2.protocol.login.InitJaggrabConnectionCodec
import org.openrs2.protocol.login.InitJs5RemoteConnectionCodec
import org.openrs2.protocol.login.IpLimitCodec
import org.openrs2.protocol.login.Js5OkCodec
import org.openrs2.protocol.login.RequestWorldListCodec
import org.openrs2.protocol.login.ServerFullCodec
import org.openrs2.protocol.world.WorldListResponseCodec

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
            InitGameConnectionCodec,
            InitJs5RemoteConnectionCodec,
            InitJaggrabConnectionCodec,
            RequestWorldListCodec,
            InitCrossDomainConnectionCodec
        )

        public val LOGIN_DOWNSTREAM: Protocol = Protocol(
            ClientOutOfDateCodec,
            ServerFullCodec,
            IpLimitCodec
        )

        /**
         * Unfortunately the Js5Ok packet's opcode overlaps with the exchange
         * session keys opcode - the only case where this happens. We therefore
         * have two LOGIN_DOWNSTREAM protocols to avoid ambiguity: one for
         * responses to the InitJs5RemoteConnection packet, and one for
         * responses to all other login packets.
         */
        public val LOGIN_DOWNSTREAM_JS5REMOTE: Protocol = Protocol(
            Js5OkCodec,
            ClientOutOfDateCodec,
            ServerFullCodec,
            IpLimitCodec
        )

        public val WORLD_LIST_DOWNSTREAM: Protocol = Protocol(
            WorldListResponseCodec
        )
    }
}
