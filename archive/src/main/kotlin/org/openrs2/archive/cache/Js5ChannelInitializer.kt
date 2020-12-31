package org.openrs2.archive.cache

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.login.ClientOutOfDateCodec
import org.openrs2.protocol.login.InitJs5RemoteConnectionCodec
import org.openrs2.protocol.login.IpLimitCodec
import org.openrs2.protocol.login.Js5OkCodec
import org.openrs2.protocol.login.ServerFullCodec

public class Js5ChannelInitializer(private val handler: Js5ChannelHandler) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            Rs2Encoder(Protocol(InitJs5RemoteConnectionCodec)),
            Rs2Decoder(Protocol(Js5OkCodec, ClientOutOfDateCodec, IpLimitCodec, ServerFullCodec)),
            handler
        )
    }
}
