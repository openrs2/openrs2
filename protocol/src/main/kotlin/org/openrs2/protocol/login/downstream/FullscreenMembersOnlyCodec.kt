package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class FullscreenMembersOnlyCodec : EmptyPacketCodec<LoginResponse.FullscreenMembersOnly>(
    packet = LoginResponse.FullscreenMembersOnly,
    opcode = 19
)
