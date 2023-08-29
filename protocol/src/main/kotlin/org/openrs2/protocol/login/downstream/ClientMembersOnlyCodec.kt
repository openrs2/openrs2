package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ClientMembersOnlyCodec : EmptyPacketCodec<LoginResponse.ClientMembersOnly>(
    packet = LoginResponse.ClientMembersOnly,
    opcode = 30
)
