package dev.openrs2.util.io

import java.util.jar.JarInputStream

val JarInputStream.entries
    get() = generateSequence { nextJarEntry }
