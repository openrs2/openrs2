package org.openrs2.archive.cache

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.ReadTimeoutHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder

public class OsrsJs5ChannelInitializer(private val handler: OsrsJs5ChannelHandler) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            ReadTimeoutHandler(30),
            Rs2Encoder(Protocol.LOGIN_UPSTREAM),
            Rs2Decoder(Protocol.JS5REMOTE_DOWNSTREAM)
        )
        ch.pipeline().addLast("handler", handler)
    }
}
