package org.openrs2.protocol

import org.openrs2.crypto.StreamCipher

object TestStreamCipher : StreamCipher {
    override fun nextInt(): Int {
        return 10
    }
}
