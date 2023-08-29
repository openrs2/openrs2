package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class UnknownReplyFromLoginServerCodec : EmptyPacketCodec<LoginResponse.UnknownReplyFromLoginServer>(
    packet = LoginResponse.UnknownReplyFromLoginServer,
    opcode = 25
)
