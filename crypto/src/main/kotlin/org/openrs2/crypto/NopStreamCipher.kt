package org.openrs2.crypto

public object NopStreamCipher : StreamCipher {
    override fun nextInt(): Int {
        return 0
    }
}
