package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class NameInvalidCodec : EmptyPacketCodec<CreateResponse.NameInvalid>(
    packet = CreateResponse.NameInvalid,
    opcode = 22
)
