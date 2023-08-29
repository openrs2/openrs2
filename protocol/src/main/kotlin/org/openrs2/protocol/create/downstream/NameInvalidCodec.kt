package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class NameInvalidCodec : EmptyPacketCodec<CreateResponse.NameInvalid>(
    packet = CreateResponse.NameInvalid,
    opcode = 22
)
