package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object ServerFullCodec : EmptyPacketCodec<LoginResponse.ServerFull>(
    packet = LoginResponse.ServerFull,
    opcode = 7
)
