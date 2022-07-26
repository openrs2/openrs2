package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CountryInvalidCodec : EmptyPacketCodec<CreateResponse.CountryInvalid>(
    packet = CreateResponse.CountryInvalid,
    opcode = 14
)
