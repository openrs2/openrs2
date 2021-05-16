package org.openrs2.archive.cache

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.ReadTimeoutHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder

public class Js5ChannelInitializer(private val handler: Js5ChannelHandler) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            ReadTimeoutHandler(30),
            Rs2Encoder(Protocol.LOGIN_UPSTREAM),
            Rs2Decoder(Protocol.LOGIN_DOWNSTREAM),
            handler
        )
    }
}
