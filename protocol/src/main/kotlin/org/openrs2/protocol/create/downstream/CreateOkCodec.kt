package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec

public class CreateOkCodec : EmptyPacketCodec<CreateResponse.Ok>(
    packet = CreateResponse.Ok,
    opcode = 2
)
