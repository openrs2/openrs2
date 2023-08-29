package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InvalidSaveCodec : EmptyPacketCodec<LoginResponse.InvalidSave>(
    packet = LoginResponse.InvalidSave,
    opcode = 13
)
