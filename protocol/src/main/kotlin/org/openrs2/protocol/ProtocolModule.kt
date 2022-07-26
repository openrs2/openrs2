package org.openrs2.protocol

import com.google.inject.AbstractModule
import com.google.inject.PrivateModule
import com.google.inject.TypeLiteral
import com.google.inject.multibindings.Multibinder
import org.openrs2.buffer.BufferModule
import org.openrs2.crypto.CryptoModule
import org.openrs2.protocol.js5.downstream.Js5ClientOutOfDateCodec
import org.openrs2.protocol.js5.downstream.Js5IpLimitCodec
import org.openrs2.protocol.js5.downstream.Js5OkCodec
import org.openrs2.protocol.js5.downstream.Js5RemoteDownstream
import org.openrs2.protocol.js5.downstream.Js5ServerFullCodec
import org.openrs2.protocol.login.downstream.ClientOutOfDateCodec
import org.openrs2.protocol.login.downstream.IpLimitCodec
import org.openrs2.protocol.login.downstream.LoginDownstream
import org.openrs2.protocol.login.downstream.ServerFullCodec
import org.openrs2.protocol.login.upstream.CheckWorldSuitabilityCodec
import org.openrs2.protocol.login.upstream.CreateAccountCodec
import org.openrs2.protocol.login.upstream.CreateCheckDateOfBirthCountryCodec
import org.openrs2.protocol.login.upstream.CreateCheckNameCodec
import org.openrs2.protocol.login.upstream.InitCrossDomainConnectionCodec
import org.openrs2.protocol.login.upstream.InitGameConnectionCodec
import org.openrs2.protocol.login.upstream.InitJaggrabConnectionCodec
import org.openrs2.protocol.login.upstream.InitJs5RemoteConnectionCodec
import org.openrs2.protocol.login.upstream.LoginUpstream
import org.openrs2.protocol.login.upstream.RequestWorldListCodec
import org.openrs2.protocol.world.downstream.WorldListDownstream
import org.openrs2.protocol.world.downstream.WorldListResponseCodec

public object ProtocolModule : AbstractModule() {
    public override fun configure() {
        install(BufferModule)
        install(CryptoModule)

        bindProtocol(
            Js5RemoteDownstream::class.java,
            Js5OkCodec::class.java,
            Js5ClientOutOfDateCodec::class.java,
            Js5ServerFullCodec::class.java,
            Js5IpLimitCodec::class.java
        )

        bindProtocol(
            LoginUpstream::class.java,
            InitGameConnectionCodec::class.java,
            InitJs5RemoteConnectionCodec::class.java,
            InitJaggrabConnectionCodec::class.java,
            CreateCheckDateOfBirthCountryCodec::class.java,
            CreateCheckNameCodec::class.java,
            CreateAccountCodec::class.java,
            RequestWorldListCodec::class.java,
            CheckWorldSuitabilityCodec::class.java,
            InitCrossDomainConnectionCodec::class.java
        )

        bindProtocol(
            LoginDownstream::class.java,
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
