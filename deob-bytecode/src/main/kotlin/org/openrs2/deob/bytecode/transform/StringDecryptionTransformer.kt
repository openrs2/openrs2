package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.deleteExpression
import org.openrs2.asm.intConstant
import org.openrs2.asm.transform.Transformer

@Singleton
public class StringDecryptionTransformer : Transformer() {
    private var stringsDecrypted = 0
    private var classesDecrypted = 0
    private var stringsInlined = 0

    override fun preTransform(classPath: ClassPath) {
        for (library in classPath.libraries) {
            for (clazz in library) {
                val clinit = clazz.methods.find { it.name == "<clinit>" } ?: continue

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

                if (inner == null || outer == null) continue

                // step 2: get the xor table from the outer method
                val switch: TableSwitchInsnNode = outer.instructions.iterator().asSequence()
                    .filterIsInstance<TableSwitchInsnNode>().firstOrNull() ?: continue

                val xorLookup = ArrayList<Number>()
                for (label in switch.labels) {
                    xorLookup.add(label.next.intConstant ?: continue)
                }
                xorLookup.add(switch.dflt.next.intConstant ?: continue)

                // step 3: decrypt all strings in the initializer
                val strings = ArrayList<String>()
                var field: FieldNode? = null
                var fieldInit: AbstractInsnNode? = null
                for (insn in clinit.instructions) {
                    if (insn.opcode != Opcodes.INVOKESTATIC) continue
                    val invoke = insn as MethodInsnNode
                    if (invoke.owner != clazz.name || invoke.name != outer.name || invoke.desc != outer.desc) continue

                    val ldc = invoke.previous.previous as LdcInsnNode
                    val str = ldc.cst as String
                    val decrypted = decryptString(str, xorLookup)

                    ldc.cst = decrypted
                    strings.add(decrypted)
                    stringsDecrypted++

                    if (field == null) {
                        if (invoke.next?.opcode == Opcodes.AASTORE && invoke.next?.next?.opcode == Opcodes.PUTSTATIC) {
                            // this one is the static String[] field
                            val put = invoke.next.next as FieldInsnNode
                            if (put.owner == clazz.name && put.desc == "[Ljava/lang/String;") {
                                field = clazz.fields.find { it.name == put.name }
                                fieldInit = put
                            }
                        } else if (invoke.next?.opcode == Opcodes.PUTSTATIC) {
                            // this one is the static String field
                            val put = invoke.next as FieldInsnNode
                            if (put.owner == clazz.name && put.desc == "Ljava/lang/String;") {
                                field = clazz.fields.find { it.name == put.name }
                                fieldInit = put
                            }
                        }
                    }

                    clinit.instructions.remove(invoke.previous)
                    clinit.instructions.remove(invoke)
                }

                // step 4: inline all string references to the static String[] field (specifically static!)
                if (field != null) {
                    for (method in clazz.methods) {
                        if (method.name == "<clinit>") continue

                        if (field.desc == "[Ljava/lang/String;") {
                            // multiple encrypted strings in the class so an array is produced
                            for (insn in method.instructions) {
                                // we're looking for getstatic -> constant -> aaload
                                // we start backwards (aaload) and check the previous 2 instructions
                                if (insn.opcode != Opcodes.AALOAD) continue

                                val push = insn.previous
                                if (push.previous.opcode != Opcodes.GETSTATIC) continue

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
                                if (insn.opcode != Opcodes.GETSTATIC) continue

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
            }
        }

        if (stringsDecrypted > 0 || classesDecrypted > 0 || stringsInlined > 0) {
            logger.info { "Decrypted $stringsDecrypted strings across $classesDecrypted classes" }
            logger.info { "Inlined $stringsInlined string references" }
        }
    }

    private fun decryptString(str: String, xorLookup: ArrayList<Number>): String {
        val chars = str.toCharArray()
        for (i in chars.indices) {
            val xor = xorLookup[i % xorLookup.size]
            chars[i] = (chars[i].code xor xor.toInt()).toChar()
        }

        return String(chars)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
