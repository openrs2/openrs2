package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class TooManyAttemptsCodec : EmptyPacketCodec<LoginResponse.TooManyAttempts>(
    packet = LoginResponse.TooManyAttempts,
    opcode = 16
)
