package org.openrs2.protocol

import com.google.inject.AbstractModule
import com.google.inject.PrivateModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.Multibinder
import org.openrs2.buffer.BufferModule
import org.openrs2.crypto.CryptoModule
import org.openrs2.protocol.js5.Js5RemoteDownstream
import org.openrs2.protocol.login.ClientOutOfDateCodec
import org.openrs2.protocol.login.InitCrossDomainConnectionCodec
import org.openrs2.protocol.login.InitGameConnectionCodec
import org.openrs2.protocol.login.InitJaggrabConnectionCodec
import org.openrs2.protocol.login.InitJs5RemoteConnectionCodec
import org.openrs2.protocol.login.IpLimitCodec
import org.openrs2.protocol.login.Js5OkCodec
import org.openrs2.protocol.login.LoginDownstream
import org.openrs2.protocol.login.LoginUpstream
import org.openrs2.protocol.login.RequestWorldListCodec
import org.openrs2.protocol.login.ServerFullCodec
import org.openrs2.protocol.world.WorldListDownstream
import org.openrs2.protocol.world.WorldListResponseCodec

public object ProtocolModule : AbstractModule() {
    public override fun configure() {
        install(BufferModule)
        install(CryptoModule)

        bindProtocol(
            LoginUpstream::class.java,
            InitGameConnectionCodec::class.java,
            InitJs5RemoteConnectionCodec::class.java,
            InitJaggrabConnectionCodec::class.java,
            RequestWorldListCodec::class.java,
            InitCrossDomainConnectionCodec::class.java
        )

        bindProtocol(
            LoginDownstream::class.java,
            ClientOutOfDateCodec::class.java,
            ServerFullCodec::class.java,
            IpLimitCodec::class.java
        )

        bindProtocol(
            Js5RemoteDownstream::class.java,
            Js5OkCodec::class.java,
            ClientOutOfDateCodec::class.java,
            ServerFullCodec::class.java,
            IpLimitCodec::class.java
        )

        bindProtocol(
            WorldListDownstream::class.java,
            WorldListResponseCodec::class.java
        )
    }

    private fun bindProtocol(
        annotation: Class<out Annotation>,
        vararg codecs: Class<out PacketCodec<*>>
    ) {
        install(object : PrivateModule() {
            override fun configure() {
                val binder = Multibinder.newSetBinder(binder(), PACKET_CODEC_TYPE_LITERAL)
                for (codec in codecs) {
                    binder.addBinding().to(codec)
                }

                bind(Protocol::class.java)
                    .annotatedWith(annotation)
                    .to(Protocol::class.java)

                expose(Protocol::class.java)
                    .annotatedWith(annotation)
            }
        })
    }

    private val PACKET_CODEC_TYPE_LITERAL = object : TypeLiteral<PacketCodec<*>>() {}
}
