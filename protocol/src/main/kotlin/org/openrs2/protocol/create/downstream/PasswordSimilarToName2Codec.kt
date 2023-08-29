package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordSimilarToName2Codec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName2>(
    packet = CreateResponse.PasswordSimilarToName2,
    opcode = 36
)
