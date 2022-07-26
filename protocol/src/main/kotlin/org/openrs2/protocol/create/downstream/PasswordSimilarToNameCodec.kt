package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class PasswordSimilarToNameCodec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName>(
    packet = CreateResponse.PasswordSimilarToName,
    opcode = 34
)
