package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CreateServerFullCodec : EmptyPacketCodec<CreateResponse.ServerFull>(
    packet = CreateResponse.ServerFull,
    opcode = 7
)
