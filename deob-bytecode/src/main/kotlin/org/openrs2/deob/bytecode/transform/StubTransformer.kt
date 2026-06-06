package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.ExtendedRemapper
import org.openrs2.asm.transform.Transformer

@Singleton
public class StubTransformer : Transformer() {
    private var mapped = 0

    override fun preTransform(classPath: ClassPath) {
        mapped = 0

        classPath.remap(object : ExtendedRemapper() {
            override fun map(internalName: String): String {
                return when (internalName) {
                    "netscape/javascript/JSObject" -> {
                        mapped++
                        "org/openrs2/jsobject/JSObject"
                    }

                    "java/util/jar/Pack200" -> {
                        mapped++
                        "org/openrs2/pack200/Pack200"
                    }

                    else -> internalName
                }
            }
        })
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Remapped $mapped references to stub classes" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
