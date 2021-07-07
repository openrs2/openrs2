package org.openrs2.archive.cache.nxt

import org.openrs2.protocol.Packet

public data class InitJs5RemoteConnection(
    public val buildMajor: Int,
    public val buildMinor: Int,
    public val token: String,
    public val language: Int
) : Packet
