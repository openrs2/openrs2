# Reference counting

[Netty][netty] uses reference counting for some objects, such as `ByteBuf`s. The
Netty documentation explains its
[use of reference counting][netty-ref-counting] in more detail.

OpenRS2 has an extension method that automatically wraps a block of code in a
`try`/`finally` block, calling `release()` in the `finally` block. It is very
similar to Kotlin's extension method for `close()`ing `Closeable`s (and in fact
has the same name: `use`).

A typical pattern for allocating and then releasing a `ByteBuf` using this
method is:

```kt
alloc.buffer().use { buf ->
    // use buf here
}
```

In OpenRS2, a method that consumes a `ByteBuf` is generally not responsible for
releasing it - the caller is. This provides more flexibility, as the caller
might want to continue reading from the buffer. (For example, after calling
`Js5Compression.uncompress()`, the caller will probably want to read the 2 byte
version trailer from the same buffer.)

For obvious reasons, a method that produces a `ByteBuf` is generally not
responsible for releasing it - again, the caller is. However, arranging for the
`ByteBuf` to be freed if an exception occurs between the `ByteBuf` being
allocated and returned is tricky. The following pattern is useful for correctly
releasing/retaining the buffer depending on whether an exception occurs or not:

```kt
alloc.buffer().use { buf ->
    // write to buf here
    return buf.retain()
}
```

If any of the code prior to the `return` fails, the buffer is released.

If the `return` is reached, no more exceptions can occur. The reference count is
increased to counteract the `finally` block decreasing it, such that by the time
the buffer reaches the caller its reference count is 1.

[netty]: https://netty.io/
[netty-ref-counting]: https://netty.io/wiki/reference-counted-objects.html
