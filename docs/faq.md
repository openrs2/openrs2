# Frequently Asked Questions

## How was build 550 chosen?

A mixture of reasons:

* The early HD era is my favourite. In particular, I like the 'clean'-looking
  tabs in the game frame, and 550 was the last build with those. I also
  considered 530, but 550 has a better built-in world map viewer (it supports
  dungeons in addition to the main surface).
* Availability of the original loader, client, game unpacker and jaggl jars.
* Availability of the complete set of original client data files.
* Availability of a large proportion of the location file encryption keys.

## Why do the OpenRS2 Natives use Maven instead of Gradle?

[nar-maven-plugin][nar-maven-plugin] is, at the time of writing, significantly
better than Gradle's support for building native code. Gradle's new C++ plugin
simply doesn't provide enough features.

## Why rewrite the client's native libraries?

Again, there are a mixture of reasons:

* Availability of the original native libraries. I struggled to find the
  original native libraries for 550, except for 32-bit Windows. While Linux and
  macOS natives are available for nearby revisions, they are not compatible
  with the 550 client.
* The original native libraries were not built for 64-bit Linux and macOS.
  While this was probably not a major problem in 2009, 64-bit architectures are
  now the norm.
* Non-x86 architectures like ARM and RISC-V are becoming more popular. If we
  start seeing a shift away from x86 on desktop machines, the native libraries
  will need to be built for those architectures.
* The original macOS jaggl native library is backed by an NSView, which was
  deprecated in Java 6 and removed in Java 7. Java 7 requires surfaces to be
  backed by a CALayer instead.
* I anticipate that at some point in the future the Linux AWT implementation
  will be ported from X11 to Wayland, which will require porting the jaggl
  native library from GLX to EGL.
* The switch away from OpenGL to newer graphics APIs like Metal and Vulkan
  might eventually necessitate the inclusion of OpenGL to Metal/Vulkan
  translation layers.
* I'm concerned about backwards compatibility and bit rot. The original native
  libraries were compiled 10 years ago, and at some point one of their
  dependencies might drop backwards binary compatibility.

## Why does OpenRS2 run its own development infrastructure?

Even though OpenRS2 does not distribute Jagex's intellectual property, there is
a risk that it could be taken down from a service like GitHub by mistake,
causing disruption. Running our own infrastructure allows us to keep backups of
data not held in the Git repository, such as issues. If necessary, we can
switch to a different hosting provider at short notice while retaining such
data.

[nar-maven-plugin]: https://maven-nar.github.io/
