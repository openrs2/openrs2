package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class DateOfBirthLastYearCodec : EmptyPacketCodec<CreateResponse.DateOfBirthLastYear>(
    packet = CreateResponse.DateOfBirthLastYear,
    opcode = 13
)
