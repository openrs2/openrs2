package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class DuplicateCodec : EmptyPacketCodec<LoginResponse.Duplicate>(
    packet = LoginResponse.Duplicate,
    opcode = 5
)
