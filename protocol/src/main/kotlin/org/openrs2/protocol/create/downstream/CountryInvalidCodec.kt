package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CountryInvalidCodec : EmptyPacketCodec<CreateResponse.CountryInvalid>(
    packet = CreateResponse.CountryInvalid,
    opcode = 14
)
