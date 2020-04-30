package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.toInternalClassName
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Singleton

@Singleton
class ClassLiteralTransformer : Transformer() {
    private val classForNameMethods = mutableListOf<MemberRef>()
    private var classLiterals = 0

    override fun preTransform(classPath: ClassPath) {
        classForNameMethods.clear()
        classLiterals = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                clazz.methods.removeIf { method ->
                    if (method.desc != "(Ljava/lang/String;)Ljava/lang/Class;") {
                        return@removeIf false
                    }

                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    }

                    val match = CLASS_FOR_NAME_MATCHER.match(method).singleOrNull() ?: return@removeIf false

                    val invokestatic = match[1] as MethodInsnNode
                    if (
                        invokestatic.owner != "java/lang/Class" ||
                        invokestatic.name != "forName" ||
                        invokestatic.desc != "(Ljava/lang/String;)Ljava/lang/Class;"
                    ) {
                        return@removeIf false
                    }

                    classForNameMethods.add(MemberRef(clazz, method))
                    return@removeIf true
                }
            }
        }

        logger.info { "Identified Class::forName methods $classForNameMethods" }
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (match in CLASS_LITERAL_MATCHER.match(method)) {
            val getstatic1 = MemberRef(match[0] as FieldInsnNode)
            val putstatic: MemberRef
            val getstatic2: MemberRef
            val invokestatic: MemberRef
            if (match[1].opcode == Opcodes.IFNONNULL) {
                putstatic = MemberRef(match[5] as FieldInsnNode)
                getstatic2 = MemberRef(match[7] as FieldInsnNode)
                invokestatic = MemberRef(match[3] as MethodInsnNode)
            } else {
                putstatic = MemberRef(match[7] as FieldInsnNode)
                getstatic2 = MemberRef(match[2] as FieldInsnNode)
                invokestatic = MemberRef(match[5] as MethodInsnNode)
            }

            if (getstatic1 != putstatic || putstatic != getstatic2) {
                continue
            }

            if (getstatic1.owner != clazz.name) {
                continue
            }

            if (invokestatic.owner != clazz.name) {
                continue
            }

            if (!classForNameMethods.contains(invokestatic)) {
                continue
            }

            for (insn in match) {
                if (insn is LdcInsnNode) {
                    insn.cst = Type.getObjectType((insn.cst as String).toInternalClassName())
                } else {
                    method.instructions.remove(insn)
                }
            }

            clazz.version = ClassVersionUtils.max(clazz.version, Opcodes.V1_5)
            clazz.fields.removeIf { it.name == getstatic1.name && it.desc == getstatic1.desc }

            classLiterals++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Updated $classLiterals class literals to Java 5 style" }
    }

    companion object {
        private val logger = InlineLogger()
        private val CLASS_FOR_NAME_MATCHER = InsnMatcher.compile(
            """
            ^ALOAD INVOKESTATIC ARETURN
            ASTORE NEW DUP (ALOAD INVOKEVIRTUAL INVOKESPECIAL | INVOKESPECIAL ALOAD INVOKEVIRTUAL) ATHROW$
        """
        )

        private const val NULL_ARM = "LDC INVOKESTATIC DUP PUTSTATIC"
        private val CLASS_LITERAL_MATCHER = InsnMatcher.compile(
            "GETSTATIC (IFNONNULL $NULL_ARM GOTO GETSTATIC | IFNULL GETSTATIC GOTO $NULL_ARM)"
        )
    }
}
