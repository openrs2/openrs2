package dev.openrs2.deob.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.Profile
import dev.openrs2.deob.remap.TypedRemapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemapTransformer @Inject constructor(private val profile: Profile) : Transformer() {
    override fun preTransform(classPath: ClassPath) {
        classPath.remap(TypedRemapper.create(classPath, profile))
    }
}
