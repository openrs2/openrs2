# OpenRS2

[![Drone][drone-badge]][drone] [![Discord][discord-badge]][discord] [![ISC license][isc-badge]][isc]

## Introduction

OpenRS2 is an open-source multiplayer game server and suite of associated tools.
It is compatible with build 550 of the RuneScape client, which was released in
late 2009.

## Prerequisites

OpenRS2 requires version 11 or later of the [Java Development Kit][jdk].

The JDK is required even if a pre-built copy of OpenRS2 is used, as it depends
on JDK-only tools, such as `jarsigner`, at runtime.

### Non-free components

OpenRS2 requires the original RuneScape client code, data and location file
encryption keys, which we cannot legally distribute.

These files must be manually placed in the `nonfree` directory (directly beneath
the root of the repository), in the following structure:

```
nonfree
└── share
    └── client
        ├── jaggl.pack200
        ├── loader_gl.jar
        ├── loader.jar
        ├── runescape_gl.pack200
        ├── runescape.jar
        └── unpackclass.pack
```

The CRC-32 checksums and SHA-256 digests of the correct files are:

| CRC-32 checksum | SHA-256 digest                                                     | File                   |
|----------------:|--------------------------------------------------------------------|------------------------|
|   `-1418094567` | `d39578f4a88a376bcb2571f05da1939a14a80d8c4ed89a4eb172d9e525795fe2` | `jaggl.pack200`        |
|   `-2129469231` | `31182683ba04dc0ad45859161c13f66424b10deb0b2df10aa58b48bba57402db` | `loader_gl.jar`        |
|   `-1516355035` | `ccdfaa86be07452ddd69f869ade86ea900dbb916fd853db16602edf2eb54211b` | `loader.jar`           |
|    `-132784534` | `4a5032ea8079d2154617ae1f21dfcc46a10e023c8ba23a4827d5e25e75c73045` | `runescape_gl.pack200` |
|    `1692522675` | `0ab28a95e7c5993860ff439ebb331c0df02ad40aa1f544777ed91b46d30d3d24` | `runescape.jar`        |
|   `-1911426584` | `7c090e07f8d754d09804ff6e9733ef3ba227893b6b639436db90977b39122590` | `unpackclass.pack`     |

The `.gitignore` file includes the `nonfree` directory to prevent any non-free
material from being accidentally included in the repository.

## Building

Run `./gradlew` to download the dependencies, build the code, run the unit tests
and package it.

IDEA 2020.3's built-in build system breaks with a cryptic
`java: java.lang.IllegalArgumentException` error message when compiling modules
that use the `deob-annotations` processor. A workaround is to add
`-Djps.track.ap.dependencies=false` to the 'Shared build process VM options' in
File -> Settings -> Build, Execution and Deployment -> Compiler. See
[IDEA-256707][idea-bug] for more information.

## Links

* [Discord][discord]
* [Issue tracker][issue-tracker]
* [KDoc][kdoc]
* [Website][www]

## License

OpenRS2 is available under the terms of the [ISC license][isc], which is similar
to the 2-clause BSD license. The full copyright notice and terms are available
in the `LICENSE` file.

[discord-badge]: https://img.shields.io/discord/684495254145335298
[discord]: https://chat.openrs2.org/
[drone-badge]: https://build.openrs2.org/api/badges/openrs2/openrs2/status.svg
[drone]: https://build.openrs2.org/openrs2/openrs2/
[idea-bug]: https://youtrack.jetbrains.com/issue/IDEA-256707
[isc-badge]: https://img.shields.io/badge/license-ISC-informational
[isc]: https://opensource.org/licenses/ISC
[issue-tracker]: https://git.openrs2.org/openrs2/openrs2/issues
[jdk]: https://jdk.java.net/
[kdoc]: https://docs.openrs2.org/
[www]: https://www.openrs2.org/
