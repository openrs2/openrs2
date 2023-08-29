package org.openrs2.protocol.js5.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class Js5IpLimitCodec : EmptyPacketCodec<Js5LoginResponse.IpLimit>(
    packet = Js5LoginResponse.IpLimit,
    opcode = 9
)
