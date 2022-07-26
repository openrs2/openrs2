package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class UpdateInProgressCodec : EmptyPacketCodec<LoginResponse.UpdateInProgress>(
    packet = LoginResponse.UpdateInProgress,
    opcode = 14
)
