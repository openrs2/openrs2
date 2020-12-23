package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object Js5OkCodec : EmptyPacketCodec<LoginResponse.Js5Ok>(
    packet = LoginResponse.Js5Ok,
    opcode = 0
)
