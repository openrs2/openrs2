package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class DateOfBirthLastYearCodec : EmptyPacketCodec<CreateResponse.DateOfBirthLastYear>(
    packet = CreateResponse.DateOfBirthLastYear,
    opcode = 13
)
