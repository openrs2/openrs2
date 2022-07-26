package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class PasswordGuessableCodec : EmptyPacketCodec<CreateResponse.PasswordGuessable>(
    packet = CreateResponse.PasswordGuessable,
    opcode = 32
)
