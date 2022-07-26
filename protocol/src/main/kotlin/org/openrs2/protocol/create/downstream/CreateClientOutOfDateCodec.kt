package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CreateClientOutOfDateCodec : EmptyPacketCodec<CreateResponse.ClientOutOfDate>(
    packet = CreateResponse.ClientOutOfDate,
    opcode = 37
)
