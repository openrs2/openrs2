package org.openrs2.archive.cache

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.ReadTimeoutHandler
import org.openrs2.archive.cache.nxt.ClientOutOfDateCodec
import org.openrs2.archive.cache.nxt.InitJs5RemoteConnectionCodec
import org.openrs2.archive.cache.nxt.Js5OkCodec
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder

public class NxtJs5ChannelInitializer(private val handler: NxtJs5ChannelHandler) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            ReadTimeoutHandler(30),
            Rs2Encoder(Protocol(InitJs5RemoteConnectionCodec)),
            Rs2Decoder(Protocol(Js5OkCodec, ClientOutOfDateCodec))
        )
        ch.pipeline().addLast("handler", handler)
    }
}
