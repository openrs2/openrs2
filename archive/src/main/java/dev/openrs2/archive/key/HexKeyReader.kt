package dev.openrs2.archive.key

import dev.openrs2.crypto.XteaKey
import java.io.InputStream

public object HexKeyReader : KeyReader {
    override fun read(input: InputStream): Sequence<XteaKey> {
        return input.bufferedReader()
            .lineSequence()
            .map(XteaKey::fromHexOrNull)
            .filterNotNull()
    }
}
