package org.openrs2.buffer

import io.netty.util.ReferenceCounted

public inline fun <T : ReferenceCounted?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this?.release()
    }
}
