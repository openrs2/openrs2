package dev.openrs2.archive.key

import dev.openrs2.crypto.XteaKey
import java.io.InputStream

public interface KeyReader {
    public fun read(input: InputStream): Sequence<XteaKey>
}
