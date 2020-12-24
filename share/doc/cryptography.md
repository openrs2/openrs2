# Cryptography

## ISAAC

[ISAAC][isaac] is a cryptographically-secure pseudorandom number generator. By
combining its output with addition/subtraction, the client uses it as a stream
cipher to encrypt the opcodes of packets.

It was implemented to break packet injection bots, such as AutoRune. While using
ISAAC in this manner only provides confidentiality, and not authenticity or
integrity, if the opcodes are tampered with the packet lengths will no longer be
in sync between the client and server. This causes one or both of the endpoints
to read garbage opcodes, though the garbage opcodes may happen to match valid
packets for a while. Eventually, one or both endpoints will detect an invalid
packet and close the connection.

## RSA

[RSA][rsa] is an asymmetric encryption and signature algorithm. The client uses
it to protect the user's password and the session's symmetric key during the
login process.

In later revisions, the JS5 master index is signed with Jagex's private key.
This change was made around the time the native libraries were moved into the
cache, ensuring that Jagex's code-signed applet could not be used to run
arbitrary native code if an attacker tampers with the JS5 connection. This
change was probably required by Jagex's certificate authority.

Jagex used a 512-bit RSA key when build 550 was released, and due to the size of
the output buffer in the client, the maximum key size is 1,008 bits. Both of
these sizes are considered insecure by modern standards, and Jagex's 512-bit
private key was factored in 2016.

Textbook RSA is used, rather than a secure padding scheme, which leads to
[several weaknesses][textbook-rsa].

## SHA-1

[SHA-1][sha1] is a cryptographic hash function. It is used to verify the
integrity of the game's code. SHA-1 is no longer secure.

## XTEA

[XTEA][xtea] is a symmetric block cipher. It is primarily used to encrypt
location files in the cache, reportedly to prevent bots from performing
path-finding across the entire map - the server only provides keys for a
location file when the player is within or adjacent to it.

It is used in [Electronic codebook (ECB)][ecb] mode. ECB is theoretically
insecure, however, as the location files are compressed before encryption it is
difficult to make use of this insecurity in practice.

The location files do not contain padding, and therefore the last 0-7 bytes are
leaked. This has no practical impact as they only contain a portion of the gzip
or bzip2 trailer.

It is also used (in ECB mode, but with padding) to encrypt the player's email
address in the create account packet, with the symmetric key encrypted with RSA.
XTEA is used as email addresses may sometimes be too long to be encrypted
directly by Jagex's 512-bit RSA key.

## Whirlpool

[Whirlpool][whirlpool] is a cryptographic hash function. It is not used in build
550, however, it is included here for completeness as it is supported by
OpenRS2's cache library. It is used to verify the integrity of native libraries
stored in the cache.

[ecb]: https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Electronic_codebook_(ECB)
[isaac]: https://burtleburtle.net/bob/rand/isaacafa.html
[rsa]: https://en.wikipedia.org/wiki/RSA_(cryptosystem)
[sha1]: https://en.wikipedia.org/wiki/SHA-1
[textbook-rsa]: https://en.wikipedia.org/wiki/RSA_(cryptosystem)#Attacks_against_plain_RSA
[whirlpool]: https://en.wikipedia.org/wiki/Whirlpool_(hash_function)
[xtea]: https://en.wikipedia.org/wiki/XTEA
