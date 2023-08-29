package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ServerFullCodec : EmptyPacketCodec<LoginResponse.ServerFull>(
    packet = LoginResponse.ServerFull,
    opcode = 7
)
