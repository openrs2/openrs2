package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class BadSessionIdCodec : EmptyPacketCodec<LoginResponse.BadSessionId>(
    packet = LoginResponse.BadSessionId,
    opcode = 10
)
