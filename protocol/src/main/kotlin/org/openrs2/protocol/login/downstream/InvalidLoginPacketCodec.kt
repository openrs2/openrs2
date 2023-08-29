package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InvalidLoginPacketCodec : EmptyPacketCodec<LoginResponse.InvalidLoginPacket>(
    packet = LoginResponse.InvalidLoginPacket,
    opcode = 22
)
