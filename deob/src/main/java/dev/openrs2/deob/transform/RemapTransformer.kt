package dev.openrs2.deob.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.TypedRemapper

class RemapTransformer : Transformer() {
    override fun preTransform(classPath: ClassPath) {
        classPath.remap(TypedRemapper.create(classPath))
    }
}
