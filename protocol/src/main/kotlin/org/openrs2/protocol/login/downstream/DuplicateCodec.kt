package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class DuplicateCodec : EmptyPacketCodec<LoginResponse.Duplicate>(
    packet = LoginResponse.Duplicate,
    opcode = 5
)
