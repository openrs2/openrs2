package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class UpdateInProgressCodec : EmptyPacketCodec<LoginResponse.UpdateInProgress>(
    packet = LoginResponse.UpdateInProgress,
    opcode = 14
)
