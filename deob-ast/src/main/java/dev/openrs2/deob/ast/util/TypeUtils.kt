package dev.openrs2.deob.ast.util

import com.github.javaparser.resolution.types.ResolvedType

fun ResolvedType.isString(): Boolean {
    return isReferenceType && asReferenceType().qualifiedName == "java.lang.String"
}
