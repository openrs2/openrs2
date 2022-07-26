package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class DateOfBirthFutureCodec : EmptyPacketCodec<CreateResponse.DateOfBirthFuture>(
    packet = CreateResponse.DateOfBirthFuture,
    opcode = 11
)
