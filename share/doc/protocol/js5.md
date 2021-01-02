# JS5 protocol

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

### Request group (prefetch/urgent)

| Data type     | Description |
|---------------|-------------|
| UnsignedByte  | Archive ID  |
| UnsignedShort | Group ID    |

Requests a group. Urgent requests have a higher priority than prefetch requests,
as the client needs the group immediately. Prefetch requests are used to
prepopulate most of the cache in the background.

### Rekey

| Data type     | Description          |
|---------------|----------------------|
| UnsignedByte  | Key                  |
| UnsignedShort | Unused (set to zero) |

Sent to set the encryption key.

### Logged in/out

Sent whenever the player logs in or out of the game. Consensus in the community
is that the logged in/out state is probably used for prioritisation, much like
the distinction between prefetch/urgent requests.

### Connected

| Data type      | Description                 |
|----------------|-----------------------------|
| UnsignedMedium | Unknown (always set to `3`) |

Sent immediately after the JS5 connection is established. Its purpose is not
known.

### Disconnect

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

After the first 512 bytes of a response, every subsequent block of 511 bytes is
prefixed with a single `0xFF` byte.
