package org.openrs2.protocol.js5.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class Js5IpLimitCodec : EmptyPacketCodec<Js5LoginResponse.IpLimit>(
    packet = Js5LoginResponse.IpLimit,
    opcode = 9
)
