package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class NeedMembersAccountCodec : EmptyPacketCodec<LoginResponse.NeedMembersAccount>(
    packet = LoginResponse.NeedMembersAccount,
    opcode = 12
)
