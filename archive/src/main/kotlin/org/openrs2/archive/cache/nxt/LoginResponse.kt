package org.openrs2.archive.cache.nxt

import org.openrs2.protocol.Packet

public sealed class LoginResponse : Packet {
    public data class Js5Ok(val loadingRequirements: List<Int>) : LoginResponse() {
        public companion object {
            public const val LOADING_REQUIREMENTS: Int = 31
        }
    }

    public object ClientOutOfDate : LoginResponse()
}
