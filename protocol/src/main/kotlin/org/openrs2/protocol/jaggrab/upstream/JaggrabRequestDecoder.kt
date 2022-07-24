package org.openrs2.protocol.jaggrab.upstream

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.MessageToMessageDecoder

@ChannelHandler.Sharable
public object JaggrabRequestDecoder : MessageToMessageDecoder<String>() {
    private const val PREFIX = "JAGGRAB "

    override fun decode(ctx: ChannelHandlerContext, msg: String, out: MutableList<Any>) {
        if (!msg.startsWith(PREFIX)) {
            throw DecoderException("JAGGRAB request has invalid prefix")
        }

        out += JaggrabRequest(msg.substring(PREFIX.length))
    }
}
