package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordGuessableCodec : EmptyPacketCodec<CreateResponse.PasswordGuessable>(
    packet = CreateResponse.PasswordGuessable,
    opcode = 32
)
