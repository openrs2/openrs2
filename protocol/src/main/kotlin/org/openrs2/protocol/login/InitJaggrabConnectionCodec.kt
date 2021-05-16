package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object InitJaggrabConnectionCodec : EmptyPacketCodec<LoginRequest.InitJaggrabConnection>(
    packet = LoginRequest.InitJaggrabConnection,
    opcode = 17
)
