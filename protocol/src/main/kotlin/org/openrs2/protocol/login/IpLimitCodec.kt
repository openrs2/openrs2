package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec

public object IpLimitCodec : EmptyPacketCodec<LoginResponse.IpLimit>(
    packet = LoginResponse.IpLimit,
    opcode = 9
)
