package org.openrs2.archive.key

import org.openrs2.crypto.SymmetricKey
import java.io.InputStream

public interface KeyReader {
    public fun read(input: InputStream): Sequence<SymmetricKey>
}
