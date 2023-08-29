package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CannotCreateAtThisTimeCodec : EmptyPacketCodec<CreateResponse.CannotCreateAtThisTime>(
    packet = CreateResponse.CannotCreateAtThisTime,
    opcode = 38
)
