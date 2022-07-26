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
import org.openrs2.protocol.login.downstream.BadSessionIdCodec
import org.openrs2.protocol.login.downstream.BannedCodec
import org.openrs2.protocol.login.downstream.ClientMembersOnlyCodec
import org.openrs2.protocol.login.downstream.ClientOutOfDateCodec
import org.openrs2.protocol.login.downstream.DisallowedByScriptCodec
import org.openrs2.protocol.login.downstream.DuplicateCodec
import org.openrs2.protocol.login.downstream.ExchangeSessionKeyCodec
import org.openrs2.protocol.login.downstream.ForcePasswordChangeCodec
import org.openrs2.protocol.login.downstream.FullscreenMembersOnlyCodec
import org.openrs2.protocol.login.downstream.HopBlockedCodec
import org.openrs2.protocol.login.downstream.InvalidLoginPacketCodec
import org.openrs2.protocol.login.downstream.InvalidLoginServerCodec
import org.openrs2.protocol.login.downstream.InvalidSaveCodec
import org.openrs2.protocol.login.downstream.InvalidUsernameOrPasswordCodec
import org.openrs2.protocol.login.downstream.IpBlockedCodec
import org.openrs2.protocol.login.downstream.IpLimitCodec
import org.openrs2.protocol.login.downstream.LockedCodec
import org.openrs2.protocol.login.downstream.LoginDownstream
import org.openrs2.protocol.login.downstream.LoginServerLoadErrorCodec
import org.openrs2.protocol.login.downstream.LoginServerOfflineCodec
import org.openrs2.protocol.login.downstream.MapMembersOnlyCodec
import org.openrs2.protocol.login.downstream.NeedMembersAccountCodec
import org.openrs2.protocol.login.downstream.NoReplyFromLoginServerCodec
import org.openrs2.protocol.login.downstream.ReconnectOkCodec
import org.openrs2.protocol.login.downstream.ServerFullCodec
import org.openrs2.protocol.login.downstream.ServiceUnavailableCodec
import org.openrs2.protocol.login.downstream.ShowVideoAdCodec
import org.openrs2.protocol.login.downstream.SwitchWorldCodec
import org.openrs2.protocol.login.downstream.TooManyAttemptsCodec
import org.openrs2.protocol.login.downstream.UnknownReplyFromLoginServerCodec
import org.openrs2.protocol.login.downstream.UpdateInProgressCodec
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
            ExchangeSessionKeyCodec::class.java,
            ShowVideoAdCodec::class.java,
            InvalidUsernameOrPasswordCodec::class.java,
            BannedCodec::class.java,
            DuplicateCodec::class.java,
            ClientOutOfDateCodec::class.java,
            ServerFullCodec::class.java,
            LoginServerOfflineCodec::class.java,
            IpLimitCodec::class.java,
            BadSessionIdCodec::class.java,
            ForcePasswordChangeCodec::class.java,
            NeedMembersAccountCodec::class.java,
            InvalidSaveCodec::class.java,
            UpdateInProgressCodec::class.java,
            ReconnectOkCodec::class.java,
            TooManyAttemptsCodec::class.java,
            MapMembersOnlyCodec::class.java,
            LockedCodec::class.java,
            FullscreenMembersOnlyCodec::class.java,
            InvalidLoginServerCodec::class.java,
            HopBlockedCodec::class.java,
            InvalidLoginPacketCodec::class.java,
            NoReplyFromLoginServerCodec::class.java,
            LoginServerLoadErrorCodec::class.java,
            UnknownReplyFromLoginServerCodec::class.java,
            IpBlockedCodec::class.java,
            ServiceUnavailableCodec::class.java,
            DisallowedByScriptCodec::class.java,
            ClientMembersOnlyCodec::class.java,
            SwitchWorldCodec::class.java
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
