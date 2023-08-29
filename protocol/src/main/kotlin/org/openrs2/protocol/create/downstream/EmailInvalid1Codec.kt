package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class EmailInvalid1Codec : EmptyPacketCodec<CreateResponse.EmailInvalid1>(
    packet = CreateResponse.EmailInvalid1,
    opcode = 42
)
