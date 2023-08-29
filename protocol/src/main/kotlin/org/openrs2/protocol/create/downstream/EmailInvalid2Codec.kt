package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class EmailInvalid2Codec : EmptyPacketCodec<CreateResponse.EmailInvalid2>(
    packet = CreateResponse.EmailInvalid2,
    opcode = 43
)
