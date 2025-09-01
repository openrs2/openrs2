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
import org.objectweb.asm.tree.MethodNode
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

    private val DECRYPTION_CALL_MATCHER = InsnMatcher.compile("LDC INVOKESTATIC INVOKESTATIC")
    private val DECRYPTION_METHOD_MATCHER = InsnMatcher.compile("(ICONST | BIPUSH) IXOR I2C CASTORE")
    private val INLINE_STRING_MATCHER = InsnMatcher.compile("GETSTATIC (ICONST | BIPUSH | SIPUSH)? AALOAD?")

    private data class DecryptionContext(
        val clinit: MethodNode,
        val inner: MethodNode,
        val outer: MethodNode,
        val strings: MutableList<String>,
        val field: FieldNode,
        val fieldInit: AbstractInsnNode
    )
    private var context: Map<String, DecryptionContext> = mutableMapOf()

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        val clinit = clazz.methods.find { it.name == "<clinit>" } ?: return false

        // step 1: identify the decryption methods
        // these methods *always* have the same signature, determined after comparing 651-668
        // we still want to check the instructions to be sure this is a no-op in other revisions
        val inner = clazz.methods.firstOrNull {
            it.name == "z" && it.desc == "(Ljava/lang/String;)[C" &&
            DECRYPTION_METHOD_MATCHER.match(it.instructions).count() > 0
        }
        val outer = clazz.methods.firstOrNull {
            it.name == "z" && it.desc == "([C)Ljava/lang/String;" &&
            DECRYPTION_METHOD_MATCHER.match(it.instructions).count() > 0
        }

        if (inner == null || outer == null) return false

        // step 2: get the xor table from the outer method
        val key = mutableListOf<Int>()
        val switch = outer.instructions.iterator().asSequence()
            .filterIsInstance<TableSwitchInsnNode>().firstOrNull() ?: return false
        for (label in switch.labels) {
            key += label.nextReal?.intConstant ?: continue
        }
        key += switch.dflt.nextReal?.intConstant ?: return false

        // step 3: decrypt all strings in the initializer
        val strings = mutableListOf<String>()
        var field: FieldNode? = null
        var fieldInit: AbstractInsnNode? = null

        for (match in DECRYPTION_CALL_MATCHER.match(clinit.instructions)) {
            val ldc = match[0] as LdcInsnNode
            val innerCall = match[1] as MethodInsnNode
            val outerCall = match[2] as MethodInsnNode

            if (innerCall.owner != clazz.name || innerCall.name != inner.name || innerCall.desc != inner.desc) {
                continue
            }

            if (outerCall.owner != clazz.name || outerCall.name != outer.name || outerCall.desc != outer.desc) {
                continue
            }

            val str = ldc.cst as String
            val decrypted = decryptString(str, key)

            ldc.cst = decrypted
            strings.add(decrypted)
            stringsDecrypted++

            if (field == null) {
                // this will run at the end of the initializer since we're checking next
                if (outerCall.next?.opcode == Opcodes.AASTORE && outerCall.next?.next?.opcode == Opcodes.PUTSTATIC) {
                    // this one is the static String[] field
                    val put = outerCall.next.next as FieldInsnNode
                    if (put.owner == clazz.name && put.desc == "[Ljava/lang/String;") {
                        field = clazz.fields.find { it.name == put.name && it.desc == put.desc }
                        fieldInit = put
                    }
                } else if (outerCall.next?.opcode == Opcodes.PUTSTATIC) {
                    // this one is the static String field
                    val put = outerCall.next as FieldInsnNode
                    if (put.owner == clazz.name && put.desc == "Ljava/lang/String;") {
                        field = clazz.fields.find { it.name == put.name && it.desc == put.desc }
                        fieldInit = put
                    }
                }
            }

            clinit.instructions.remove(innerCall)
            clinit.instructions.remove(outerCall)
        }

        if (field != null && fieldInit != null) {
            context += clazz.name to DecryptionContext(clinit, inner, outer, strings, field, fieldInit)
        }

        classesDecrypted++
        return false
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val ctx = context[clazz.name] ?: return false

        // step 4: inline string references
        for (match in INLINE_STRING_MATCHER.match(method.instructions)) {
            val getstatic = match[0] as FieldInsnNode
            if (
                getstatic.owner != clazz.name ||
                getstatic.name != ctx.field.name ||
                getstatic.desc != ctx.field.desc
            ) {
                continue
            }

            if (ctx.field.desc == "[Ljava/lang/String;") {
                val index = match[1].intConstant ?: continue
                val str = ctx.strings[index]
                val ldc = LdcInsnNode(str)

                method.instructions.remove(match[2])
                method.instructions.remove(match[1])
                method.instructions.set(getstatic, ldc)
            } else if (ctx.field.desc == "Ljava/lang/String;") {
                val str = ctx.strings[0]
                val ldc = LdcInsnNode(str)

                method.instructions.set(getstatic, ldc)
            }

            stringsInlined++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        for (library in classPath.libraries) {
            for (clazz in library) {
                val ctx = context[clazz.name] ?: continue

                // step 5: cleanup
                ctx.clinit.instructions.deleteExpression(ctx.fieldInit)
                clazz.fields.remove(ctx.field)
                clazz.methods.remove(ctx.inner)
                clazz.methods.remove(ctx.outer)
            }
        }

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
