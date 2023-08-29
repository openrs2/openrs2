package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordInvalidCharsCodec : EmptyPacketCodec<CreateResponse.PasswordInvalidChars>(
    packet = CreateResponse.PasswordInvalidChars,
    opcode = 31
)
