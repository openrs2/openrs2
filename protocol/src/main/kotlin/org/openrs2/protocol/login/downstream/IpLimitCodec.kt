package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class IpLimitCodec : EmptyPacketCodec<LoginResponse.IpLimit>(
    packet = LoginResponse.IpLimit,
    opcode = 9
)
