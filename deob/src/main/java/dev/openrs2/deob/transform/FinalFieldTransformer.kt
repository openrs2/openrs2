package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.analysis.FieldWriteAnalyzer
import dev.openrs2.deob.analysis.FieldWriteCount
import dev.openrs2.deob.analysis.ThisInterpreter
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer

class FinalFieldTransformer : Transformer() {
    private lateinit var inheritedFieldSets: DisjointSet<MemberRef>
    private val nonFinalFields = mutableSetOf<DisjointSet.Partition<MemberRef>>()

    override fun preTransform(classPath: ClassPath) {
        inheritedFieldSets = classPath.createInheritedFieldSets()
        nonFinalFields.clear()
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val constructor = method.name == "<init>" || method.name == "<clinit>"
        val thisCall = if (constructor && method.name == "<init>") {
            method.instructions.filterIsInstance<MethodInsnNode>()
                .any { it.opcode == Opcodes.INVOKESPECIAL && it.owner == clazz.name && it.name == "<init>" }
        } else {
            false
        }

        val constructorWithoutThisCall = constructor && !thisCall

        val frames = if (constructorWithoutThisCall) {
            Analyzer(ThisInterpreter()).analyze(clazz.name, method)
        } else {
            null
        }

        for (insn in method.instructions) {
            if (insn !is FieldInsnNode) {
                continue
            } else if (insn.opcode != Opcodes.PUTFIELD && insn.opcode != Opcodes.PUTSTATIC) {
                continue
            }

            if (constructorWithoutThisCall) {
                val isThis = if (insn.opcode == Opcodes.PUTFIELD) {
                    val insnIndex = method.instructions.indexOf(insn)
                    val frame = frames!![insnIndex] ?: continue
                    frame.getStack(frame.stackSize - 2).isThis
                } else {
                    true
                }

                val declaredOwner = classPath[insn.owner]!!.resolveField(MemberDesc(insn))!!.name
                if (isThis && declaredOwner == clazz.name) {
                    /*
                     * Writes inside constructors without a this(...) call to
                     * fields owned by the same class are analyzed separately - if
                     * there is exactly one write in the constructor the field can
                     * be made final.
                     */
                    continue
                }
            }

            val partition = inheritedFieldSets[MemberRef(insn)] ?: continue
            nonFinalFields += partition
        }

        if (constructorWithoutThisCall) {
            val analyzer = FieldWriteAnalyzer(clazz, method, classPath, frames!!)
            analyzer.analyze()

            val exits = method.instructions.filter { it.opcode == Opcodes.RETURN }
            for (insn in exits) {
                val counts = analyzer.getOutSet(insn) ?: emptyMap()

                for (field in clazz.fields) {
                    val count = counts.getOrDefault(MemberDesc(field), FieldWriteCount.NEVER)
                    if (count != FieldWriteCount.EXACTLY_ONCE) {
                        val partition = inheritedFieldSets[MemberRef(clazz, field)]!!
                        nonFinalFields += partition
                    }
                }
            }
        }

        return false
    }

    override fun transformField(classPath: ClassPath, library: Library, clazz: ClassNode, field: FieldNode): Boolean {
        if ((field.access and Opcodes.ACC_VOLATILE) != 0) {
            val partition = inheritedFieldSets[MemberRef(clazz, field)]!!
            nonFinalFields += partition
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        var fieldsChanged = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (field in clazz.fields) {
                    val member = MemberRef(clazz, field)
                    val partition = inheritedFieldSets[member]!!

                    val access = field.access

                    if (nonFinalFields.contains(partition)) {
                        field.access = field.access and Opcodes.ACC_FINAL.inv()
                    } else {
                        field.access = field.access or Opcodes.ACC_FINAL
                    }

                    if (field.access != access) {
                        fieldsChanged++
                    }
                }
            }
        }

        logger.info { "Updated final modifier on $fieldsChanged fields" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}