package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class DateOfBirthFutureCodec : EmptyPacketCodec<CreateResponse.DateOfBirthFuture>(
    packet = CreateResponse.DateOfBirthFuture,
    opcode = 11
)
