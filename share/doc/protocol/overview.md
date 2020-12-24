# Protocol overview

The RuneScape protocol runs on top of the [Transmission Control Protocol
(TCP)][tcp]. As TCP is stream-oriented, a framing scheme is required to
determine where one packet ends and where the next begins. RuneScape primarily
uses a [type-length-value (TLV)][tlv] scheme for framing, though some exceptions
do exist - for example, the JAGGRAB and JS5 protocols use different framing
schemes.

A RuneScape connection may be in several states, each of which has a different
set of opcodes (types in TLV parlance). Opcodes may have different meanings in
different states. Opcodes may be optionally encrypted with
the [ISAAC stream cipher][isaac].

All connections start in the [login](login.md) state.

## Types

There are three types of packets:

* Fixed length - supports any payload length, including no payload.
* Variable length (byte) - supports up to 255 bytes of payload.
* Variable length (short) - supports up to 65,535 bytes of payload in theory,
  though in practice the client's receive buffer limits the payload to 5,000
  bytes in the 550 build. This limit is raised by OpenRS2's client patcher.

They are structured as follows:

### Fixed length

| Data type    | Description |
|--------------|-------------|
| UnsignedByte | Opcode      |
| Byte\[len\]  | Payload     |

### Variable length (byte)

| Data type    | Description  |
|--------------|--------------|
| UnsignedByte | Opcode       |
| UnsignedByte | Length (len) |
| Byte\[len\]  | Payload      |

### Variable length (short)

| Data type     | Description  |
|---------------|--------------|
| UnsignedByte  | Opcode       |
| UnsignedShort | Length (len) |
| Byte\[len\]   | Payload      |

## Opcode encryption

Opcodes are encrypted by adding the opcode to the next 32-bit output from the
ISAAC random number generator and truncating the result to 8 bits. Decryption is
the same process in reverse: the 32-bit output is subtracted from the encrypted
opcode, and then the result is truncated to 8 bits.

There are two ISAAC random number generators: one used for upstream (client ->
server) traffic and one used for downstream (server -> client) traffic. Both are
initialised with a seed consisting of four 32-bit integers.

The upstream seed is the same as the downstream seed, except each integer
component is incremented by 50.

The seed is negotiated during the login process. Before it is negotiated,
opcodes are not encrypted.

[isaac]: https://burtleburtle.net/bob/rand/isaacafa.html
[tcp]: https://en.wikipedia.org/wiki/Transmission_Control_Protocol
[tlv]: https://en.wikipedia.org/wiki/Type-length-value
