package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class NameUnavailableCodec : EmptyPacketCodec<CreateResponse.NameUnavailable>(
    packet = CreateResponse.NameUnavailable,
    opcode = 20
)
