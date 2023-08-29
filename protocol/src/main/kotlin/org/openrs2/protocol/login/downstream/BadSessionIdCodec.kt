package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class BadSessionIdCodec : EmptyPacketCodec<LoginResponse.BadSessionId>(
    packet = LoginResponse.BadSessionId,
    opcode = 10
)
