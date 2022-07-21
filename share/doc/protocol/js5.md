# JS5 protocol

The JS5 remote protocol is used by the client to download groups of files from
the server as they are required, as well as prefetching certain groups in
advance. Groups contain the client's assets and scripts, and are stored in the
cache.

The client opens a connection to the primary game server port and sends the
`INIT_JS5REMOTE_CONNECTION` packet, which consists of a single byte opcode
(`15`) followed by the client's build number as an integer.

The server responds with a single `0` byte if the connection attempt is
successful. It may also respond with `CLIENT_OUT_OF_DATE` (`6`), `SERVER_FULL`
(`7`) or `IP_LIMIT` (`9`), after which the connection will be closed.

After a successful connection attempt, the connection remains in JS5 mode for
the rest of its life.

JS5 is a request/response protocol with support for pipelining - the 550 client
is capable of handling up to 20 prefetch and 20 urgent in-flight requests.

## Upstream

| Opcode | Description              |
|-------:|--------------------------|
|      0 | Request group (prefetch) |
|      1 | Request group (urgent)   |
|      2 | Logged in                |
|      3 | Logged out               |
|      4 | Rekey                    |
|      6 | Connected                |
|      7 | Disconnect               |

All upstream packets are exactly 4 bytes long, including the opcode. Unused
payload bytes are set to zero.

### 0 (Request group (prefetch))

| Data type     | Description |
|---------------|-------------|
| UnsignedByte  | Archive ID  |
| UnsignedShort | Group ID    |

Requests a group. Prefetch requests are used to  prepopulate most of the cache
in the background.

### 1 (Request group (urgent))

| Data type     | Description |
|---------------|-------------|
| UnsignedByte  | Archive ID  |
| UnsignedShort | Group ID    |

Requests a group. Urgent requests have a higher priority than prefetch requests,
as the client needs the group immediately.

### 2 (Logged in)

Sent whenever the player logs into the game. Consensus in the community is that
the logged in/out state is probably used for prioritisation, much like the
distinction between prefetch/urgent requests.

### 3 (Logged out)

Sent whenever the player logs out of the game.

### 4 (Rekey)

| Data type     | Description          |
|---------------|----------------------|
| UnsignedByte  | Key                  |
| UnsignedShort | Unused (set to zero) |

Sent to set the encryption key.

### 6 (Connected)

| Data type      | Description                 |
|----------------|-----------------------------|
| UnsignedMedium | Unknown (always set to `3`) |

Sent immediately after the JS5 connection is established. Its purpose is not
known.

### 7 (Disconnect)

Requests that the server closes the connection. Sent by the `::serverjs5drop`
command.

## Downstream

There is only a single type of response packet, and as such, there are no
response opcodes. The packet contains a group requested by the client.

| Data type      | Description                                               |
|----------------|-----------------------------------------------------------|
| UnsignedByte   | Archive ID                                                |
| UnsignedShort  | Group ID                                                  |
| UnsignedByte   | Compression type (ORed with `0x80` for prefetch requests) |
| Int            | Compressed length                                         |
| Int (optional) | Uncompressed length (present iff the group is compressed) |
| Byte[]         | Compressed data (with `0xFF` markers, see below)          |

If encryption is enabled, all downstream bytes are XORed with the encryption
key. The client enables encryption if the checksum of a group does not match the
expected checksum in the master index or index.

The encryption scheme is probably used to defend against middle boxes tampering
with the plaintext traffic.

After the first 512 bytes of a response (including the header), every subsequent
block of 511 bytes is prefixed with a single `0xFF` byte.
