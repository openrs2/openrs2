# OpenRS2

[![Build status badge](https://build.openrs2.dev/buildStatus/icon?job=openrs2&build=lastCompleted)](https://build.openrs2.dev/job/openrs2/)

## Introduction

OpenRS2 is an open-source multiplayer game server and suite of associated
tools. It is compatible with build 550 of the RuneScape client, which was
released in late 2009.

## Prerequisites

Building OpenRS2 requires the following pieces of software:

* [Java Development Kit][jdk] (version 11)
* [Apache Maven][maven] (version 3.3.9 or later)

### Non-free components

OpenRS2 requires the original RuneScape client code, data and location file
encryption keys, which we cannot legally distribute.

These files must be manually placed in the `nonfree` directory (directly
beneath the root of the repository), in the following structure:

```
nonfree
└── code
    ├── game_unpacker.dat
    ├── jaggl.pack200
    ├── loader_gl.jar
    ├── loader.jar
    ├── runescape_gl.pack200
    └── runescape.jar
```

The SHA-256 checksums of the correct files are:

```
7c090e07f8d754d09804ff6e9733ef3ba227893b6b639436db90977b39122590  nonfree/code/game_unpacker.dat
d39578f4a88a376bcb2571f05da1939a14a80d8c4ed89a4eb172d9e525795fe2  nonfree/code/jaggl.pack200
31182683ba04dc0ad45859161c13f66424b10deb0b2df10aa58b48bba57402db  nonfree/code/loader_gl.jar
ccdfaa86be07452ddd69f869ade86ea900dbb916fd853db16602edf2eb54211b  nonfree/code/loader.jar
4a5032ea8079d2154617ae1f21dfcc46a10e023c8ba23a4827d5e25e75c73045  nonfree/code/runescape_gl.pack200
0ab28a95e7c5993860ff439ebb331c0df02ad40aa1f544777ed91b46d30d3d24  nonfree/code/runescape.jar
```

The `nonfree` directory is included in the `.gitignore` file to prevent any
non-free material from being accidentally included in the repository.

## Building

Run `mvn verify` to download the dependencies, build the code, run the unit
tests and package it.

## Contributing

### Code style

All source code must be formatted with [IntelliJ IDEA][idea]'s built-in
formatter before each commit. The 'Optimize imports' option should also be
selected. Do not select 'Rearrange entries'.

OpenRS2's code style settings are held in `.idea/codeStyles/Project.xml` in the
repository, and IDEA should use them automatically after importing the Maven
project.

### Commit messages

Commit messages should follow the ['seven rules'][commitmsg] described in
'How to Write a Git Commit Message', with the exception that the summary line
can be up to 72 characters in length (as OpenRS2 does not use email-based
patches).

## License

Unless otherwise stated, all code and data is licensed under version 3.0 (and
only version 3.0) of the [GNU General Public License][gpl]. The full terms are
available in the `COPYING` file.

The `deob-annotations` module is instead licensed under version 3.0 (and only
version 3.0) of the [GNU Lesser General Public License][lgpl]. The full terms
are available in the `COPYING.LESSER` file in the module's directory.

## Copyright

Copyright (c) 2019 OpenRS2 Authors

OpenRS2 is free software: you can redistribute it and/or modify it under the
terms of version 3.0 of the GNU General Public License as published by the Free
Software Foundation.

OpenRS2 is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
OpenRS2. If not, see <https://www.gnu.org/licenses/>.

[commitmsg]: https://chris.beams.io/posts/git-commit/#seven-rules
[gpl]: https://www.gnu.org/licenses/gpl-3.0.html
[idea]: https://www.jetbrains.com/idea/
[jdk]: https://jdk.java.net/
[lgpl]: https://www.gnu.org/licenses/lgpl-3.0.html
[maven]: https://maven.apache.org/
