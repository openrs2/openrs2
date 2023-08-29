package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CreateIpLimitCodec : EmptyPacketCodec<CreateResponse.IpLimit>(
    packet = CreateResponse.IpLimit,
    opcode = 9
)
