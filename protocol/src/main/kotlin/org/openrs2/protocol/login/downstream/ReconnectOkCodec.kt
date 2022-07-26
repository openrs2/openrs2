package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ReconnectOkCodec : EmptyPacketCodec<LoginResponse.ReconnectOk>(
    packet = LoginResponse.ReconnectOk,
    opcode = 15
)
