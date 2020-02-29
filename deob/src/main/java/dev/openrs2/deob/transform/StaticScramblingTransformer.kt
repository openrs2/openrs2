package dev.openrs2.deob.transform

import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.TypedRemapper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class StaticScramblingTransformer : Transformer() {
    private val fields = mutableMapOf<MemberRef, String>()
    private val methods = mutableMapOf<MemberRef, String>()
    private var nextStaticClass: ClassNode? = null
    private val staticClasses = mutableListOf<ClassNode>()

    private fun nextClass(): ClassNode {
        var clazz = nextStaticClass
        if (clazz != null && (clazz.fields.size + clazz.methods.size) < MAX_FIELDS_AND_METHODS) {
            return clazz
        }

        clazz = ClassNode()
        clazz.version = Opcodes.V1_1
        clazz.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
        clazz.name = "Static${staticClasses.size + 1}"
        clazz.superName = "java/lang/Object"
        clazz.interfaces = mutableListOf()
        clazz.innerClasses = mutableListOf()
        clazz.fields = mutableListOf()
        clazz.methods = mutableListOf()

        staticClasses += clazz
        nextStaticClass = clazz

        return clazz
    }

    override fun preTransform(classPath: ClassPath) {
        fields.clear()
        methods.clear()
        nextStaticClass = null
        staticClasses.clear()

        for (library in classPath.libraries) {
            // TODO(gpe): improve detection of the client library
            if ("client" !in library) {
                continue
            }

            for (clazz in library) {
                // TODO(gpe): exclude the JSObject class
                if (clazz.name in TypedRemapper.EXCLUDED_CLASSES) {
                    continue
                }

                clazz.methods.removeIf { method ->
                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    } else if (method.access and Opcodes.ACC_NATIVE != 0) {
                        return@removeIf false
                    } else if (method.name in TypedRemapper.EXCLUDED_METHODS) {
                        return@removeIf false
                    }

                    val staticClass = nextClass()
                    staticClass.methods.add(method)
                    staticClass.version = ClassVersionUtils.maxVersion(staticClass.version, clazz.version)

                    methods[MemberRef(clazz, method)] = staticClass.name
                    return@removeIf true
                }
            }

            for (clazz in staticClasses) {
                library.add(clazz)
            }
        }
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            when (insn) {
                is FieldInsnNode -> insn.owner = fields.getOrDefault(MemberRef(insn), insn.owner)
                is MethodInsnNode -> insn.owner = methods.getOrDefault(MemberRef(insn), insn.owner)
            }
        }

        return false
    }

    companion object {
        private const val MAX_FIELDS_AND_METHODS = 500
    }
}
