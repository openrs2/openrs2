package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ClientMembersOnlyCodec : EmptyPacketCodec<LoginResponse.ClientMembersOnly>(
    packet = LoginResponse.ClientMembersOnly,
    opcode = 30
)
