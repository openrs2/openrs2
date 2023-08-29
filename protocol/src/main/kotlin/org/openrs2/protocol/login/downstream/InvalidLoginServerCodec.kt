package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InvalidLoginServerCodec : EmptyPacketCodec<LoginResponse.InvalidLoginServer>(
    packet = LoginResponse.InvalidLoginServer,
    opcode = 20
)
