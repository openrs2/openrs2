package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class DateOfBirthThisYearCodec : EmptyPacketCodec<CreateResponse.DateOfBirthThisYear>(
    packet = CreateResponse.DateOfBirthThisYear,
    opcode = 12
)
