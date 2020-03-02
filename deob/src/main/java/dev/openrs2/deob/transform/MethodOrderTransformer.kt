package dev.openrs2.deob.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class MethodOrderTransformer : Transformer() {
    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        clazz.methods.sortWith(STATIC_COMPARATOR.then(INIT_COMPARATOR))
        return false
    }

    companion object {
        private val STATIC_COMPARATOR = Comparator<MethodNode> { a, b ->
            val aStatic = a.access and Opcodes.ACC_STATIC != 0
            val bStatic = b.access and Opcodes.ACC_STATIC != 0
            when {
                aStatic && !bStatic -> -1
                !aStatic && bStatic -> 1
                else -> 0
            }
        }

        private val INIT_COMPARATOR = Comparator<MethodNode> { a, b ->
            val aInit = a.name.startsWith('<')
            val bInit = b.name.startsWith('<')
            when {
                aInit && !bInit -> -1
                !aInit && bInit -> 1
                else -> 0
            }
        }
    }
}
