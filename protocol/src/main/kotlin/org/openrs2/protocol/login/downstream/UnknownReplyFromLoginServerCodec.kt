package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class UnknownReplyFromLoginServerCodec : EmptyPacketCodec<LoginResponse.UnknownReplyFromLoginServer>(
    packet = LoginResponse.UnknownReplyFromLoginServer,
    opcode = 25
)
