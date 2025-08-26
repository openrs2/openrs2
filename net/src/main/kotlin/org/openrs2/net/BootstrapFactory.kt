package org.openrs2.net

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.reflect.KClass

@Singleton
public class BootstrapFactory @Inject constructor(
    private val alloc: ByteBufAllocator
) {
    public fun createBootstrap(group: EventLoopGroup, channel: KClass<out Channel>): Bootstrap {
        return Bootstrap()
            .group(group)
            .channel(channel.java)
            .option(ChannelOption.ALLOCATOR, alloc)
            .option(ChannelOption.AUTO_READ, false)
            .option(ChannelOption.TCP_NODELAY, true)
    }

    public fun createServerBootstrap(group: EventLoopGroup, channel: KClass<out ServerChannel>): ServerBootstrap {
        return ServerBootstrap()
            .group(group)
            .channel(channel.java)
            .option(ChannelOption.ALLOCATOR, alloc)
            .childOption(ChannelOption.ALLOCATOR, alloc)
            .childOption(ChannelOption.AUTO_READ, false)
            .childOption(ChannelOption.TCP_NODELAY, true)
    }
}
