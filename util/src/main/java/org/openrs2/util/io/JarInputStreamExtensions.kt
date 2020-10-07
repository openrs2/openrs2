package org.openrs2.util.io

import java.util.jar.JarEntry
import java.util.jar.JarInputStream

public val JarInputStream.entries: Sequence<JarEntry>
    get() = generateSequence { nextJarEntry }
