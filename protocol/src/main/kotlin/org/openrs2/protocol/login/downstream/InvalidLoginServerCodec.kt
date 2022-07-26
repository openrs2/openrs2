package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InvalidLoginServerCodec : EmptyPacketCodec<LoginResponse.InvalidLoginServer>(
    packet = LoginResponse.InvalidLoginServer,
    opcode = 20
)
