package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class NoReplyFromLoginServerCodec : EmptyPacketCodec<LoginResponse.NoReplyFromLoginServer>(
    packet = LoginResponse.NoReplyFromLoginServer,
    opcode = 23
)
