package org.openrs2.util.io

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public val ZipInputStream.entries: Sequence<ZipEntry>
    get() = generateSequence { nextEntry }
