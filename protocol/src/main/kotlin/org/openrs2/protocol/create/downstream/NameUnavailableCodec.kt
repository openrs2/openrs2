package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class NameUnavailableCodec : EmptyPacketCodec<CreateResponse.NameUnavailable>(
    packet = CreateResponse.NameUnavailable,
    opcode = 20
)
