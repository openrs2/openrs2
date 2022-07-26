package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CreateServerFullCodec : EmptyPacketCodec<CreateResponse.ServerFull>(
    packet = CreateResponse.ServerFull,
    opcode = 7
)
