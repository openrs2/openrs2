package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class BannedCodec : EmptyPacketCodec<LoginResponse.Banned>(
    packet = LoginResponse.Banned,
    opcode = 4
)
