package org.openrs2.protocol.login

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ServerFullCodec : EmptyPacketCodec<LoginResponse.ServerFull>(
    packet = LoginResponse.ServerFull,
    opcode = 7
)
