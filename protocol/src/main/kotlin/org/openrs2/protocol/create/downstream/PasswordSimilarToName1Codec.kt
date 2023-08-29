package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class PasswordSimilarToName1Codec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName1>(
    packet = CreateResponse.PasswordSimilarToName1,
    opcode = 35
)
