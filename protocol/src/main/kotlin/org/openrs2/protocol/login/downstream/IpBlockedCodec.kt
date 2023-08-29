package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class IpBlockedCodec : EmptyPacketCodec<LoginResponse.IpBlocked>(
    packet = LoginResponse.IpBlocked,
    opcode = 26
)
