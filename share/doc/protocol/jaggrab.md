# JAGGRAB protocol

JAGGRAB is a very simple protocol used by the loader to fetch the game's code.
It is only used as a fallback if fetching the code via HTTP fails. It is very
similar to [HTTP/0.9][http09] - the first version of HTTP.

The loader opens a connection to the secondary game server port (443) and sends
the 'switch to JAGGRAB mode' packet. As this packet has no payload, it only
contains a single opcode byte: `17`.

The loader then writes the following string:

    JAGGRAB <file name>

where `<file name>` is replaced with the name of the file to fetch. The string
is followed by two line feed characters.

The server responds with the requested file and then closes the connection.

The file names are suffixed with their CRC-32 checksum (for example,
`unpackclass.pack` -> `unpackclass_-1911426584.pack`). The same file names are
used when the client requests the files over HTTP, so the suffixes are
presumably for cache busting. The Old School RuneScape servers do not require
the checksum suffix to be present or correct.

In build 550, the following files may be requested with JAGGRAB:

| Remote file name       | Local file name        | Description                                        |
|------------------------|------------------------|----------------------------------------------------|
| `unpackclass.pack`     | `game_unpacker.dat`    | packclass unpacker                                 |
| `runescape.pack200`    | `main_file_cache.dat0` | SD client (pack200 format)                         |
| `runescape.js5`        | `main_file_cache.dat1` | SD client (packclass format)                       |
| `runescape_gl.pack200` | `main_file_cache.dat3` | HD client (pack200 format)                         |
| `runescape_gl.js5`     | `main_file_cache.dat4` | HD client (packclass format)                       |
| `jaggl.pack200`        | `main_file_cache.dat5` | OpenGL bindings (pack200 format)                   |
| `jaggl.js5`            | `main_file_cache.dat6` | OpenGL bindings (packclass format)                 |
| `jaggl_0_0.lib`        | `jaggl.dll`            | OpenGL native library (Windows i386)               |
| `jaggl_1_0.lib`        | `libjaggl.so`          | OpenGL native library (Linux i386)                 |
| `jaggl_1_1.lib`        | `libjaggl_dri.so`      | DRI hack native library (Linux i386)               |
| `jaggl_2_0.lib`        | `libjaggl.jnilib`      | OpenGL native library (macOS PowerPC)              |
| `jaggl_3_0.lib`        | `libjaggl.jnilib`      | OpenGL native library (macOS i386)                 |
| `jaggl_4_0.lib`        | `jaggl.dll`            | OpenGL native library (Windows amd64)              |
| `jagmisc_0.lib`        | `jagmisc.dll`          | Miscellaneous native library (Windows i386)        |
| `jagmisc_1.lib`        | `jagmiscms.dll`        | Miscellaneous native library (Windows i386, MSJVM) |
| `jagmisc_2.lib`        | `jagmisc64.dll`        | Miscellaneous native library (Windows amd64)       |

Note that OpenRS2's client patcher changes the list of supported platforms:

* MSJVM support is removed.
* PowerPC support is removed.
* amd64 support is added to macOS and Linux.
* AArch64 supported is added to macOS.

This causes some differences to the list of supported files.

It also normalises some of the local file names (for example, `jagmisc64.dll` ->
`jagmisc.dll`, `libjaggl.jnilib` -> `libjaggl.dylib`).

[http09]: https://www.w3.org/Protocols/HTTP/AsImplemented.html
