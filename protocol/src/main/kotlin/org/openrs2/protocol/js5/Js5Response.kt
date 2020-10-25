package org.openrs2.protocol.js5

import io.netty.buffer.ByteBuf
import io.netty.util.ReferenceCounted

public data class Js5Response(
    public val prefetch: Boolean,
    public val archive: Int,
    public val group: Int,
    public val data: ByteBuf
) : ReferenceCounted {
    override fun refCnt(): Int {
        return data.refCnt()
    }

    override fun retain(): Js5Response {
        data.retain()
        return this
    }

    override fun retain(increment: Int): Js5Response {
        data.retain(increment)
        return this
    }

    override fun touch(): Js5Response {
        data.touch()
        return this
    }

    override fun touch(hint: Any?): Js5Response {
        data.touch(hint)
        return this
    }

    override fun release(): Boolean {
        return data.release()
    }

    override fun release(decrement: Int): Boolean {
        return data.release(decrement)
    }
}
