package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CreateClientOutOfDateCodec : EmptyPacketCodec<CreateResponse.ClientOutOfDate>(
    packet = CreateResponse.ClientOutOfDate,
    opcode = 37
)
