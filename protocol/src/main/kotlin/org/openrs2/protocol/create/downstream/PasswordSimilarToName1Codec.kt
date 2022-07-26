package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class PasswordSimilarToName1Codec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName1>(
    packet = CreateResponse.PasswordSimilarToName1,
    opcode = 35
)
