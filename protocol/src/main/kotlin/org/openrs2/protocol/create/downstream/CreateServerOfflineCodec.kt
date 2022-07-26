package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class CreateServerOfflineCodec : EmptyPacketCodec<CreateResponse.CreateServerOffline>(
    packet = CreateResponse.CreateServerOffline,
    opcode = 3
)
