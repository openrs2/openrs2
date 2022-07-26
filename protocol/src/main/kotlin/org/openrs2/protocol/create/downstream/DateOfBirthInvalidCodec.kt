package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class DateOfBirthInvalidCodec : EmptyPacketCodec<CreateResponse.DateOfBirthInvalid>(
    packet = CreateResponse.DateOfBirthInvalid,
    opcode = 10
)
