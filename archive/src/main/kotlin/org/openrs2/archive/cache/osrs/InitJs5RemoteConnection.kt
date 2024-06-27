package org.openrs2.archive.cache.osrs

import org.openrs2.crypto.SymmetricKey
import org.openrs2.protocol.Packet

public data class InitJs5RemoteConnection(
    public val build: Int,
    public val key: SymmetricKey,
) : Packet
