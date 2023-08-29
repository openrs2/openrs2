package org.openrs2.protocol.js5.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class Js5ServerFullCodec : EmptyPacketCodec<Js5LoginResponse.ServerFull>(
    packet = Js5LoginResponse.ServerFull,
    opcode = 7
)
