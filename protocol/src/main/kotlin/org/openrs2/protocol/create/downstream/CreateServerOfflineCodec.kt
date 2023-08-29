package org.openrs2.protocol.create.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class CreateServerOfflineCodec : EmptyPacketCodec<CreateResponse.CreateServerOffline>(
    packet = CreateResponse.CreateServerOffline,
    opcode = 3
)
