package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InvalidUsernameOrPasswordCodec : EmptyPacketCodec<LoginResponse.InvalidUsernameOrPassword>(
    packet = LoginResponse.InvalidUsernameOrPassword,
    opcode = 3
)
