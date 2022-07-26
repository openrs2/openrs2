package org.openrs2.protocol.js5.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class Js5ServerFullCodec : EmptyPacketCodec<Js5LoginResponse.ServerFull>(
    packet = Js5LoginResponse.ServerFull,
    opcode = 7
)
