package org.openrs2.protocol.jaggrab

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

@ChannelHandler.Sharable
public object JaggrabRequestEncoder : MessageToMessageEncoder<JaggrabRequest>() {
    override fun encode(ctx: ChannelHandlerContext, msg: JaggrabRequest, out: MutableList<Any>) {
        out += "JAGGRAB ${msg.path}\n\n"
    }
}
