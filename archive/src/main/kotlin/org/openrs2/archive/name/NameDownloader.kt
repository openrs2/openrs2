package org.openrs2.archive.name

public interface NameDownloader {
    public suspend fun download(): Sequence<String>
}
