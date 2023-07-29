package org.openrs2.crypto

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import java.security.MessageDigest

public fun ByteBuf.sha1(): ByteArray {
    return sha1(readerIndex(), readableBytes())
}

public fun ByteBuf.sha1(index: Int, len: Int): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    if (hasArray()) {
        digest.update(array(), arrayOffset() + index, len)
    } else {
        digest.update(ByteBufUtil.getBytes(this, index, len, false))
    }
    return digest.digest()
}
