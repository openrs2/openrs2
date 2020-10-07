package org.openrs2.deob.transform

import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.Profile
import org.openrs2.deob.remap.TypedRemapper
import org.openrs2.deob.util.map.NameMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class RemapTransformer @Inject constructor(
    private val profile: Profile,
    private val nameMap: NameMap
) : Transformer() {
    override fun preTransform(classPath: ClassPath) {
        classPath.remap(TypedRemapper.create(classPath, profile, nameMap))
    }
}
