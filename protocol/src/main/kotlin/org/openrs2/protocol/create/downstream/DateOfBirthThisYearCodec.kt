package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class DateOfBirthThisYearCodec : EmptyPacketCodec<CreateResponse.DateOfBirthThisYear>(
    packet = CreateResponse.DateOfBirthThisYear,
    opcode = 12
)
