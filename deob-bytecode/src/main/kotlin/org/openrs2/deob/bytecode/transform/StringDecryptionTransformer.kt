package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.deleteExpression
import org.openrs2.asm.intConstant
import org.openrs2.asm.nextReal
import org.openrs2.asm.transform.Transformer

@Singleton
public class StringDecryptionTransformer : Transformer() {
    private var stringsDecrypted = 0
    private var classesDecrypted = 0
    private var stringsInlined = 0

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        val clinit = clazz.methods.find { it.name == "<clinit>" } ?: return false

        // step 1: identify the decryption methods
        val inner = clinit.instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>()
            .filter {
                it.owner == clazz.name && it.desc == "(Ljava/lang/String;)[C" &&
                    it.next?.opcode == Opcodes.INVOKESTATIC
            }.firstOrNull()?.let {
                clazz.methods.find { method -> method.name == it.name && method.desc == it.desc }
            }
        val outer = clinit.instructions.iterator().asSequence().filterIsInstance<MethodInsnNode>()
            .filter {
                it.owner == clazz.name && it.desc == "([C)Ljava/lang/String;" &&
                    it.previous?.opcode == Opcodes.INVOKESTATIC
            }.firstOrNull()?.let {
                clazz.methods.find { method -> method.name == it.name && method.desc == it.desc }
            }

        if (inner == null || outer == null) return false

        // step 2: get the xor table from the outer method
        val switch = outer.instructions.iterator().asSequence()
            .filterIsInstance<TableSwitchInsnNode>().firstOrNull() ?: return false

        val key = mutableListOf<Int>()
        for (label in switch.labels) {
            key += label.nextReal?.intConstant ?: continue
        }
        key += switch.dflt.nextReal?.intConstant ?: return false

        // step 3: decrypt all strings in the initializer
        val strings = mutableListOf<String>()
        var field: FieldNode? = null
        var fieldInit: AbstractInsnNode? = null
        for (insn in clinit.instructions) {
            if (insn !is MethodInsnNode || insn.opcode != Opcodes.INVOKESTATIC) {
                continue
            }

            if (insn.owner != clazz.name || insn.name != outer.name || insn.desc != outer.desc) {
                continue
            }

            val ldc = insn.previous.previous as LdcInsnNode
            val str = ldc.cst as String
            val decrypted = decryptString(str, key)

            ldc.cst = decrypted
            strings.add(decrypted)
            stringsDecrypted++

            if (field == null) {
                if (insn.next?.opcode == Opcodes.AASTORE && insn.next?.next?.opcode == Opcodes.PUTSTATIC) {
                    // this one is the static String[] field
                    val put = insn.next.next as FieldInsnNode
                    if (put.owner == clazz.name && put.desc == "[Ljava/lang/String;") {
                        field = clazz.fields.find { it.name == put.name }
                        fieldInit = put
                    }
                } else if (insn.next?.opcode == Opcodes.PUTSTATIC) {
                    // this one is the static String field
                    val put = insn.next as FieldInsnNode
                    if (put.owner == clazz.name && put.desc == "Ljava/lang/String;") {
                        field = clazz.fields.find { it.name == put.name }
                        fieldInit = put
                    }
                }
            }

            clinit.instructions.remove(insn.previous)
            clinit.instructions.remove(insn)
        }

        // step 4: inline all string references to the static String[] field (specifically static!)
        if (field != null) {
            for (method in clazz.methods) {
                if (field.desc == "[Ljava/lang/String;") {
                    // multiple encrypted strings in the class so an array is produced
                    for (insn in method.instructions) {
                        // we're looking for getstatic -> constant -> aaload
                        // we start backwards (aaload) and check the previous 2 instructions
                        if (insn.opcode != Opcodes.AALOAD) {
                            continue
                        }

                        val push = insn.previous
                        if (push.previous.opcode != Opcodes.GETSTATIC) {
                            continue
                        }

                        val getstatic = push.previous as FieldInsnNode
                        if (
                            getstatic.owner != clazz.name ||
                            getstatic.name != field.name ||
                            getstatic.desc != field.desc
                        ) {
                            continue
                        }

                        val index = push.intConstant ?: continue
                        val str = strings[index]
                        val ldc = LdcInsnNode(str)

                        method.instructions.remove(push)
                        method.instructions.remove(insn)
                        method.instructions.set(getstatic, ldc)

                        stringsInlined++
                    }
                } else if (field.desc == "Ljava/lang/String;") {
                    // one encrypted string in the class so no array is produced
                    for (insn in method.instructions) {
                        if (insn.opcode != Opcodes.GETSTATIC) {
                            continue
                        }

                        val getstatic = insn as FieldInsnNode
                        if (
                            getstatic.owner != clazz.name ||
                            getstatic.name != field.name ||
                            getstatic.desc != field.desc
                        ) {
                            continue
                        }

                        val str = strings[0]
                        val ldc = LdcInsnNode(str)

                        method.instructions.set(getstatic, ldc)

                        stringsInlined++
                    }
                }
            }

            // step 5: cleanup
            clazz.fields.remove(field)
            clinit.instructions.deleteExpression(fieldInit!!)
        }

        // step 5: cleanup
        clazz.methods.remove(inner)
        clazz.methods.remove(outer)

        classesDecrypted++
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Decrypted $stringsDecrypted strings across $classesDecrypted classes" }
        logger.info { "Inlined $stringsInlined string references" }
    }

    private fun decryptString(str: String, key: List<Int>): String {
        val chars = str.toCharArray()
        for (i in chars.indices) {
            val value = key[i % key.size]
            chars[i] = (chars[i].code xor value).toChar()
        }

        return String(chars)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
