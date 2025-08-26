package org.openrs2.game.net

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleStateHandler
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import org.openrs2.game.net.login.LoginChannelHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.login.downstream.LoginDownstream
import org.openrs2.protocol.login.upstream.LoginUpstream
import java.util.concurrent.TimeUnit

@Singleton
public class Rs2ChannelInitializer @Inject constructor(
    private val handlerProvider: Provider<LoginChannelHandler>,
    @param:LoginUpstream
    private val loginUpstreamProtocol: Protocol,
    @param:LoginDownstream
    private val loginDownstreamProtocol: Protocol
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            IdleStateHandler(true, TIMEOUT_SECS, TIMEOUT_SECS, TIMEOUT_SECS, TimeUnit.SECONDS),
            Rs2Decoder(loginUpstreamProtocol),
            Rs2Encoder(loginDownstreamProtocol),
            handlerProvider.get()
        )
    }

    private companion object {
        /*
         * On the OSRS servers, login connections take 60 seconds to time out,
         * but a JS5 connection only takes 30 seconds. It'd be awkward for us
         * to change the timeouts at runtime to fully emulate that behaviour.
         */
        private const val TIMEOUT_SECS: Long = 30
    }
}
