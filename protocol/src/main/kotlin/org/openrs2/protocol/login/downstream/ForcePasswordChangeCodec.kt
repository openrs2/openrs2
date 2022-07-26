package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class ForcePasswordChangeCodec : EmptyPacketCodec<LoginResponse.ForcePasswordChange>(
    packet = LoginResponse.ForcePasswordChange,
    opcode = 11
)
