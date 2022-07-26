package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class LockedCodec : EmptyPacketCodec<LoginResponse.Locked>(
    packet = LoginResponse.Locked,
    opcode = 18
)
