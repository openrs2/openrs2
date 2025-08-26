package org.openrs2.net

import io.netty.channel.Channel
import io.netty.channel.IoHandlerFactory
import io.netty.channel.ServerChannel
import kotlin.reflect.KClass

public data class Transport(
    val ioHandlerFactory: IoHandlerFactory,
    val socketChannel: KClass<out Channel>,
    val serverSocketChannel: KClass<out ServerChannel>,
)
