package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CannotCreateAtThisTimeCodec : EmptyPacketCodec<CreateResponse.CannotCreateAtThisTime>(
    packet = CreateResponse.CannotCreateAtThisTime,
    opcode = 38
)
