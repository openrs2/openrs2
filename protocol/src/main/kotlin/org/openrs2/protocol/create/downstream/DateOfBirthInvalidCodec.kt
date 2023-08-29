package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class DateOfBirthInvalidCodec : EmptyPacketCodec<CreateResponse.DateOfBirthInvalid>(
    packet = CreateResponse.DateOfBirthInvalid,
    opcode = 10
)
