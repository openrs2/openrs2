package org.openrs2.archive.cache.nxt

import org.openrs2.protocol.EmptyPacketCodec

public object ClientOutOfDateCodec : EmptyPacketCodec<LoginResponse.ClientOutOfDate>(
    opcode = 6,
    packet = LoginResponse.ClientOutOfDate
)
