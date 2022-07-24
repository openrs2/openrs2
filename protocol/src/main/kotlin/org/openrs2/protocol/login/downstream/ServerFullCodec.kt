package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ServerFullCodec : EmptyPacketCodec<LoginResponse.ServerFull>(
    packet = LoginResponse.ServerFull,
    opcode = 7
)
