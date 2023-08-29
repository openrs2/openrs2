package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class EmailInvalidCodec : EmptyPacketCodec<CreateResponse.EmailInvalid>(
    packet = CreateResponse.EmailInvalid,
    opcode = 41
)
