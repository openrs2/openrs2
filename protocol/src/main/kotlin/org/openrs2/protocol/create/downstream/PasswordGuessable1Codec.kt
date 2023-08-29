package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordGuessable1Codec : EmptyPacketCodec<CreateResponse.PasswordGuessable1>(
    packet = CreateResponse.PasswordGuessable1,
    opcode = 33
)
