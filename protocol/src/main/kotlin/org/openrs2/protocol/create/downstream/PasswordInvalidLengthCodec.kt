package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordInvalidLengthCodec : EmptyPacketCodec<CreateResponse.PasswordInvalidLength>(
    packet = CreateResponse.PasswordInvalidLength,
    opcode = 30
)
