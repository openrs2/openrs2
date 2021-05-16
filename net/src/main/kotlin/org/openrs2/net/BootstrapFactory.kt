package org.openrs2.net

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.incubator.channel.uring.IOUring
import io.netty.incubator.channel.uring.IOUringEventLoopGroup
import io.netty.incubator.channel.uring.IOUringServerSocketChannel
import io.netty.incubator.channel.uring.IOUringSocketChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class BootstrapFactory @Inject constructor(
    private val alloc: ByteBufAllocator
) {
    public fun createEventLoopGroup(): EventLoopGroup {
        return when {
            IOUring.isAvailable() -> IOUringEventLoopGroup()
            Epoll.isAvailable() -> EpollEventLoopGroup()
            KQueue.isAvailable() -> KQueueEventLoopGroup()
            else -> NioEventLoopGroup()
        }
    }

    public fun createBootstrap(group: EventLoopGroup): Bootstrap {
        val channel = when (group) {
            is IOUringEventLoopGroup -> IOUringSocketChannel::class.java
            is EpollEventLoopGroup -> EpollSocketChannel::class.java
            is KQueueEventLoopGroup -> KQueueSocketChannel::class.java
            is NioEventLoopGroup -> NioSocketChannel::class.java
            else -> throw IllegalArgumentException("Unknown EventLoopGroup type")
        }

        return Bootstrap()
            .group(group)
            .channel(channel)
            .option(ChannelOption.ALLOCATOR, alloc)
            .option(ChannelOption.AUTO_READ, false)
            .option(ChannelOption.TCP_NODELAY, true)
    }

    public fun createServerBootstrap(group: EventLoopGroup): ServerBootstrap {
        val channel = when (group) {
            is IOUringEventLoopGroup -> IOUringServerSocketChannel::class.java
            is EpollEventLoopGroup -> EpollServerSocketChannel::class.java
            is KQueueEventLoopGroup -> KQueueServerSocketChannel::class.java
            is NioEventLoopGroup -> NioServerSocketChannel::class.java
            else -> throw IllegalArgumentException("Unknown EventLoopGroup type")
        }

        return ServerBootstrap()
            .group(group)
            .channel(channel)
            .option(ChannelOption.ALLOCATOR, alloc)
            .childOption(ChannelOption.ALLOCATOR, alloc)
            .childOption(ChannelOption.AUTO_READ, false)
            .childOption(ChannelOption.TCP_NODELAY, true)
    }
}
