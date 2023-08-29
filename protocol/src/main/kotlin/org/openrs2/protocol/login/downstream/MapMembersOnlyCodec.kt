package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class MapMembersOnlyCodec : EmptyPacketCodec<LoginResponse.MapMembersOnly>(
    packet = LoginResponse.MapMembersOnly,
    opcode = 17
)
