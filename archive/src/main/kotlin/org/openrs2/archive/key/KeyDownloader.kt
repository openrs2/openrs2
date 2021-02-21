package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey

public interface KeyDownloader {
    public suspend fun getMissingUrls(seenUrls: Set<String>): Set<String>
    public suspend fun download(url: String): Sequence<XteaKey>
}
