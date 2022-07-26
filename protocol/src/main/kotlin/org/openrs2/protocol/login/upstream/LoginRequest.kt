package org.openrs2.protocol.login.upstream

import org.openrs2.protocol.Packet

public sealed class LoginRequest : Packet {
    public data class InitGameConnection(public val usernameHash: Int) : LoginRequest()
    public data class InitJs5RemoteConnection(public val build: Int) : LoginRequest()
    public object InitJaggrabConnection : LoginRequest()
    public data class CreateCheckDateOfBirthCountry(
        public val year: Int,
        public val month: Int,
        public val day: Int,
        public val country: Int
    ) : LoginRequest()
    public data class CreateCheckName(public val username: String) : LoginRequest()
    public data class CreateAccount(
        public val build: Int,
        public val gameNewsletters: Boolean,
        public val otherNewsletters: Boolean,
        public val shareDetailsWithBusinessPartners: Boolean,
        public val username: String,
        public val password: String,
        public val affiliate: Int,
        public val year: Int,
        public val month: Int,
        public val day: Int,
        public val country: Int,
        public val email: String
    ) : LoginRequest()
    public data class RequestWorldList(public val checksum: Int) : LoginRequest()
    public data class CheckWorldSuitability(
        public val build: Int,
        public val username: String,
        public val password: String
    ) : LoginRequest()
    public object InitCrossDomainConnection : LoginRequest()
}
