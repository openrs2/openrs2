package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CreateIpLimitCodec : EmptyPacketCodec<CreateResponse.IpLimit>(
    packet = CreateResponse.IpLimit,
    opcode = 9
)
