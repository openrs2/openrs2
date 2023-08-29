package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class NeedMembersAccountCodec : EmptyPacketCodec<LoginResponse.NeedMembersAccount>(
    packet = LoginResponse.NeedMembersAccount,
    opcode = 12
)
