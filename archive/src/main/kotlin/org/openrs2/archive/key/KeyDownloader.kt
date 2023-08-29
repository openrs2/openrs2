package org.openrs2.archive.key

import org.openrs2.crypto.SymmetricKey

public abstract class KeyDownloader(
    public val source: KeySource
) {
    public abstract suspend fun getMissingUrls(seenUrls: Set<String>): Set<String>
    public abstract suspend fun download(url: String): Sequence<SymmetricKey>
}
