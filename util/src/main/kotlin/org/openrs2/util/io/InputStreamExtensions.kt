package org.openrs2.util.io

import java.io.InputStream
import java.util.Arrays

public fun InputStream.contentEquals(other: InputStream): Boolean {
    val buf1 = ByteArray(4096)
    val buf2 = ByteArray(4096)

    while (true) {
        val n1 = read(buf1, 0, buf1.size)
        if (n1 == -1) {
            return other.read() == -1
        }

        var off = 0
        var remaining = n1
        while (remaining > 0) {
            val n2 = other.read(buf2, off, remaining)
            if (n2 == -1) {
                return false
            }

            off += n2
            remaining -= n2
        }

        if (!Arrays.equals(buf1, 0, n1, buf2, 0, n1)) {
            return false
        }
    }
}
