package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class EmailInvalid1Codec : EmptyPacketCodec<CreateResponse.EmailInvalid1>(
    packet = CreateResponse.EmailInvalid1,
    opcode = 42
)
