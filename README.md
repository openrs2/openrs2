# OpenRS2

## Introduction

OpenRS2 is an open-source multiplayer game server and suite of associated
tools. It is compatible with build 550 of the RuneScape client, which was
released in late 2009.

## Prerequisites

Building OpenRS2 requires the following pieces of software on all platforms:

* [Java Development Kit][jdk] (version 12 or later)
* [Apache Maven][maven] (version 3.3.9 or later)

### Platform-specific dependencies

OpenRS2 includes open-source replacements for the jaggl, jaggl_dri and jagmisc
native libraries.

Your platform's standard C compiler and [OpenGL][opengl] development headers
must be installed to build the native libraries.

Alternatively, pass `-P '!windows,!mac,!unix'` to all `mvn` invocations to
exclude the native libraries from the build.

#### Windows

Install the [Build Tools for Visual Studio][visualstudio] (2019 or later),
which provide a command-line version of the Visual C++ compiler. The full GUI
version of Visual Studio will also work, but it is not required.

The Visual C++ tools must be present on your `%PATH%` when building OpenRS2.
The easiest way to achieve this is to open the command prompt with the
'Developer Command Prompt for VS 2019' item from the start menu.

The Java Development Kit and Apache Maven `bin` directories must be manually
[added to your `%PATH%` variable][path].

#### macOS

Run `xcode-select --install` from the terminal to install the [Xcode][xcode]
command-line tools.

The easiest way to install the Java Development Kit and Apache Maven is with the
[Homebrew][homebrew] package manager. After installing Homebrew, run
`brew cask install java` and `brew install maven`.

#### UNIX

OpenRS2 requires the [GNU Compiler Collection][gcc] and the [Mesa 3D Graphics
Library][mesa] on UNIX-like systems (e.g. Linux).

The easiest way to install all the dependencies is with your system's package
manager. For example:

* Debian-based systems: `apt install openjdk-12-jdk maven gcc libgl1-mesa-dev`
* RPM-based systems: `yum install java-12-openjdk-devel maven gcc mesa-libGL-devel`
* ArchLinux-based systems: `pacman -S jdk-openjdk maven gcc mesa`

As OpenRS2 depends on a modern version of Java (at the time of writing), you
will probably need to use a similarly modern version of your Linux distribution
or enable its backports repository.

`-headless` packages are not sufficient as the `gl-natives` module is linked
with `libjawt`.

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

The `deob-annotations`, `gl-natives`, `gl-dri-natives` and `misc-natives`
modules are instead licensed under version 3.0 (and only version 3.0) of the
[GNU Lesser General Public License][lgpl]. The full terms are available in the
`COPYING.LESSER` file in each module's directory.

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
[gcc]: https://gcc.gnu.org/
[gpl]: https://www.gnu.org/licenses/gpl-3.0.html
[homebrew]: https://brew.sh/
[idea]: https://www.jetbrains.com/idea/
[jdk]: https://jdk.java.net/
[lgpl]: https://www.gnu.org/licenses/lgpl-3.0.html
[maven]: https://maven.apache.org/
[mesa]: https://www.mesa3d.org/
[opengl]: https://www.opengl.org/
[path]: https://www.java.com/en/download/help/path.xml
[runescape]: https://www.runescape.com/
[visualstudio]: https://visualstudio.microsoft.com/downloads/
[xcode]: https://developer.apple.com/xcode/
