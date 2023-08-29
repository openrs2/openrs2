package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class InvalidUsernameOrPasswordCodec : EmptyPacketCodec<LoginResponse.InvalidUsernameOrPassword>(
    packet = LoginResponse.InvalidUsernameOrPassword,
    opcode = 3
)
