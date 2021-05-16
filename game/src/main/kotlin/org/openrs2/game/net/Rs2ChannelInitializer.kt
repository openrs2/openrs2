package org.openrs2.game.net

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import org.openrs2.game.net.login.LoginChannelHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
public class Rs2ChannelInitializer @Inject constructor(
    private val handlerProvider: Provider<LoginChannelHandler>
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            Rs2Decoder(Protocol.LOGIN_UPSTREAM),
            Rs2Encoder(Protocol.LOGIN_DOWNSTREAM),
            handlerProvider.get()
        )
    }
}
