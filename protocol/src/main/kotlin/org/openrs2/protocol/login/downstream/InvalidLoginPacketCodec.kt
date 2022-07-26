package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InvalidLoginPacketCodec : EmptyPacketCodec<LoginResponse.InvalidLoginPacket>(
    packet = LoginResponse.InvalidLoginPacket,
    opcode = 22
)
