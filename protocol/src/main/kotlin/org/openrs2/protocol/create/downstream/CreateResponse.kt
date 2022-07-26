package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.Packet

public sealed class CreateResponse : Packet {
    public object Ok : CreateResponse()
    public object CreateServerOffline : CreateResponse()
    public object ServerFull : CreateResponse()
    public object IpLimit : CreateResponse()
    public object DateOfBirthInvalid : CreateResponse()
    public object DateOfBirthFuture : CreateResponse()
    public object DateOfBirthThisYear : CreateResponse()
    public object DateOfBirthLastYear : CreateResponse()
    public object CountryInvalid : CreateResponse()
    public object NameUnavailable : CreateResponse()
    public data class NameSuggestions(val names: List<String>) : CreateResponse()
    public object NameInvalid : CreateResponse()
    public object PasswordInvalidLength : CreateResponse()
    public object PasswordInvalidChars : CreateResponse()
    public object PasswordGuessable : CreateResponse()
    public object PasswordGuessable1 : CreateResponse()
    public object PasswordSimilarToName : CreateResponse()
    public object PasswordSimilarToName1 : CreateResponse()
    public object PasswordSimilarToName2 : CreateResponse()
    public object ClientOutOfDate : CreateResponse()
    public object CannotCreateAtThisTime : CreateResponse()
    public object EmailInvalid : CreateResponse()
    public object EmailInvalid1 : CreateResponse()
    public object EmailInvalid2 : CreateResponse()
}
