package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object ClientOutOfDateCodec : EmptyPacketCodec<LoginResponse.ClientOutOfDate>(
    packet = LoginResponse.ClientOutOfDate,
    opcode = 6
)
