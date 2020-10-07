package org.openrs2.deob.ast.util

import com.github.javaparser.resolution.types.ResolvedType

public fun ResolvedType.isString(): Boolean {
    return isReferenceType && asReferenceType().qualifiedName == "java.lang.String"
}

public fun ResolvedType.isClass(): Boolean {
    return isReferenceType && asReferenceType().qualifiedName == "java.lang.Class"
}
