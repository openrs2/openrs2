package org.openrs2.archive.web

import com.jakewharton.byteunits.BinaryByteUnit

public object ByteUnits {
    public fun format(value: Long): String {
        return BinaryByteUnit.format(value).replace(" ", "\u00A0")
    }
}
