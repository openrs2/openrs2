package org.openrs2.archive.cache

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.ReadTimeoutHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.js5.downstream.Js5ClientOutOfDateCodec
import org.openrs2.protocol.js5.downstream.Js5OkCodec
import org.openrs2.protocol.login.upstream.InitJs5RemoteConnectionCodec

public class OsrsJs5ChannelInitializer(private val handler: OsrsJs5ChannelHandler) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            ReadTimeoutHandler(30),
            Rs2Encoder(Protocol(InitJs5RemoteConnectionCodec())),
            Rs2Decoder(Protocol(Js5OkCodec(), Js5ClientOutOfDateCodec()))
        )
        ch.pipeline().addLast("handler", handler)
    }
}
