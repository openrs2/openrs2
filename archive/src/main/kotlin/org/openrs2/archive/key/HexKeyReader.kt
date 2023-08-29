package org.openrs2.archive.key

import org.openrs2.crypto.SymmetricKey
import java.io.InputStream

public object HexKeyReader : KeyReader {
    override fun read(input: InputStream): Sequence<SymmetricKey> {
        return input.bufferedReader()
            .lineSequence()
            .map(SymmetricKey::fromHexOrNull)
            .filterNotNull()
    }
}
