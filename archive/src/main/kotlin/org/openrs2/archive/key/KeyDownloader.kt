package org.openrs2.archive.key

import org.openrs2.crypto.XteaKey

public interface KeyDownloader {
    public suspend fun download(): Sequence<XteaKey>
}
