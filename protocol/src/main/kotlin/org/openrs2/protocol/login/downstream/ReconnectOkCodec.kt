package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ReconnectOkCodec : EmptyPacketCodec<LoginResponse.ReconnectOk>(
    packet = LoginResponse.ReconnectOk,
    opcode = 15
)
