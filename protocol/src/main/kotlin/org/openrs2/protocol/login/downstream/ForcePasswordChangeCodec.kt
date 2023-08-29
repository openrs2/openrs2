package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ForcePasswordChangeCodec : EmptyPacketCodec<LoginResponse.ForcePasswordChange>(
    packet = LoginResponse.ForcePasswordChange,
    opcode = 11
)
