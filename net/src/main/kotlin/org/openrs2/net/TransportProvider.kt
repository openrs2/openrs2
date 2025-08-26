package org.openrs2.net

import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.kqueue.KQueueSocketChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.uring.IoUring
import io.netty.channel.uring.IoUringIoHandler
import io.netty.channel.uring.IoUringServerSocketChannel
import io.netty.channel.uring.IoUringSocketChannel
import jakarta.inject.Provider

public class TransportProvider : Provider<Transport> {
    override fun get(): Transport {
        return when {
            IoUring.isAvailable() -> Transport(
                IoUringIoHandler.newFactory(),
                IoUringSocketChannel::class,
                IoUringServerSocketChannel::class,
            )
            Epoll.isAvailable() -> Transport(
                EpollIoHandler.newFactory(),
                EpollSocketChannel::class,
                EpollServerSocketChannel::class,
            )
            KQueue.isAvailable() -> Transport(
                KQueueIoHandler.newFactory(),
                KQueueSocketChannel::class,
                KQueueServerSocketChannel::class,
            )
            else -> Transport(
                NioIoHandler.newFactory(),
                NioSocketChannel::class,
                NioServerSocketChannel::class,
            )
        }
    }
}
