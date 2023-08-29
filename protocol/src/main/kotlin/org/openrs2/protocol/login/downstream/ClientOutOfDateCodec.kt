package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ClientOutOfDateCodec : EmptyPacketCodec<LoginResponse.ClientOutOfDate>(
    packet = LoginResponse.ClientOutOfDate,
    opcode = 6
)
