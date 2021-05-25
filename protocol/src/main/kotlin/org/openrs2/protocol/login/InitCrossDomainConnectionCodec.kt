package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object InitCrossDomainConnectionCodec : EmptyPacketCodec<LoginRequest.InitCrossDomainConnection>(
    opcode = 'G'.code,
    packet = LoginRequest.InitCrossDomainConnection
)
