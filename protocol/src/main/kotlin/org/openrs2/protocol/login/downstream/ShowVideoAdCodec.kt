package org.openrs2.protocol.login.downstream

import jakarta.inject.Singleton
import org.openrs2.protocol.EmptyPacketCodec

@Singleton
public class ShowVideoAdCodec : EmptyPacketCodec<LoginResponse.ShowVideoAd>(
    packet = LoginResponse.ShowVideoAd,
    opcode = 1
)
