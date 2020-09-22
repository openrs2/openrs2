package dev.openrs2.archive.key

import dev.openrs2.crypto.XteaKey
import java.io.InputStream

public object TextKeyReader : KeyReader {
    override fun read(input: InputStream): Sequence<XteaKey> {
        val reader = input.bufferedReader()

        val k0 = reader.readLine().toIntOrNull() ?: return emptySequence()
        val k1 = reader.readLine().toIntOrNull() ?: return emptySequence()
        val k2 = reader.readLine().toIntOrNull() ?: return emptySequence()
        val k3 = reader.readLine().toIntOrNull() ?: return emptySequence()

        return sequenceOf(XteaKey(k0, k1, k2, k3))
    }
}
