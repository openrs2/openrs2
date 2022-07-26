package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class IpBlockedCodec : EmptyPacketCodec<LoginResponse.IpBlocked>(
    packet = LoginResponse.IpBlocked,
    opcode = 26
)
