package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.buffer.DefaultByteBufHolder

public data class Js5Response(
    public val prefetch: Boolean,
    public val archive: Int,
    public val group: Int,
    public val data: ByteBuf
) : DefaultByteBufHolder(data)
