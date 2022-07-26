package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class MapMembersOnlyCodec : EmptyPacketCodec<LoginResponse.MapMembersOnly>(
    packet = LoginResponse.MapMembersOnly,
    opcode = 17
)
