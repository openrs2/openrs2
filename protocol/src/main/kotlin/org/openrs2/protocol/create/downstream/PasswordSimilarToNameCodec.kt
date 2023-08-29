package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordSimilarToNameCodec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName>(
    packet = CreateResponse.PasswordSimilarToName,
    opcode = 34
)
