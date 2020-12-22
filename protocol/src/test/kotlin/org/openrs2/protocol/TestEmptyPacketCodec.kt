package org.openrs2.protocol

object TestEmptyPacketCodec : EmptyPacketCodec<EmptyPacket>(
    packet = EmptyPacket,
    opcode = 5
)
