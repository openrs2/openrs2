package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class EmailInvalidCodec : EmptyPacketCodec<CreateResponse.EmailInvalid>(
    packet = CreateResponse.EmailInvalid,
    opcode = 41
)
