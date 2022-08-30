package org.openrs2.archive.cache.nxt

import org.openrs2.protocol.EmptyPacketCodec

public object Js5OkCodec : EmptyPacketCodec<LoginResponse.Js5Ok>(
    opcode = 0,
    packet = LoginResponse.Js5Ok
)
