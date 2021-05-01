package org.openrs2.buffer.generator

public enum class Transformation(
    public val suffix: String,
    public val preTransform: String,
    public val postReadTransform: String,
    public val postWriteTransform: String
) {
    IDENTITY("", "", "", ""),
    A("A", "", " - 128", " + 128"),
    C("C", "-", "", ""),
    S("S", "128 - ", "", "")
}
