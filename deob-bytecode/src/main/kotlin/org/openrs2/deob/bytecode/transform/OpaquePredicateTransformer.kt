package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.hasCode
import org.openrs2.asm.transform.Transformer

@Singleton
public class OpaquePredicateTransformer : Transformer() {
    private val flowObstructors = mutableSetOf<MemberRef>()
    private var assignments = 0
    private var opaquePredicates = 0

    override fun preTransform(classPath: ClassPath) {
        flowObstructors.clear()
        assignments = 0
        opaquePredicates = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (method.hasCode) {
                        findFlowObstructors(library, method)
                    }
                }
            }
        }

        logger.info { "Identified flow obstructors $flowObstructors" }
    }

    private fun findFlowObstructors(library: Library, method: MethodNode) {
        for (match in FLOW_OBSTRUCTOR_INITIALIZER_MATCHER.match(method)) {
            val putstatic = match.last() as FieldInsnNode

            /*
             * if the initializer reads a local variable, check the local
             * variable is populated with a static field
             */
            val first = match.first()
            if (first is VarInsnNode) {
                val storeFound = STORE_MATCHER.match(method).any {
                    val istore = it[1] as VarInsnNode
                    return@any first.`var` == istore.`var`
                }

                if (!storeFound) {
                    continue
                }
            }

            // add flow obstructor to set
            flowObstructors.add(MemberRef(putstatic))

            /*
             * remove initializer (except the opaque predicate at the start,
             * which we treat like any other)
             */
            match.drop(2).forEach(method.instructions::remove)

            // remove field
            val owner = library[putstatic.owner]!!
            owner.fields.removeIf { it.name == putstatic.name && it.desc == putstatic.desc }
        }
    }

    private fun isFlowObstructor(insn: FieldInsnNode): Boolean {
        return MemberRef(insn) in flowObstructors
    }

    private fun isOpaquePredicate(method: MethodNode, match: List<AbstractInsnNode>): Boolean {
        val load = match[0]

        // flow obstructor loaded directly?
        if (load is FieldInsnNode && load.opcode == Opcodes.GETSTATIC) {
            return isFlowObstructor(load)
        }

        // flow obstructor loaded via local variable
        val iload = load as VarInsnNode
        return STORE_MATCHER.match(method).any {
            val getstatic = it[0] as FieldInsnNode
            val istore = it[1] as VarInsnNode
            return@any isFlowObstructor(getstatic) && iload.`var` == istore.`var`
        }
    }

    private fun isRedundantPut(match: List<AbstractInsnNode>): Boolean {
        val putstatic = match[1] as FieldInsnNode
        return isFlowObstructor(putstatic)
    }

    private fun isRedundantStore(match: List<AbstractInsnNode>): Boolean {
        val getstatic = match[0] as FieldInsnNode
        return isFlowObstructor(getstatic)
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        // find and fix opaque predicates
        for (match in OPAQUE_PREDICATE_MATCHER.match(method).filter { isOpaquePredicate(method, it) }) {
            val branch = match[1] as JumpInsnNode
            when (branch.opcode) {
                Opcodes.IFEQ -> {
                    // branch is always taken
                    method.instructions.remove(match[0])
                    branch.opcode = Opcodes.GOTO
                }

                Opcodes.IFNE -> {
                    // branch is never taken
                    match.forEach(method.instructions::remove)
                }

                else -> error("Invalid opcode")
            }

            opaquePredicates++
        }

        // remove redundant stores
        for (match in STORE_MATCHER.match(method).filter(::isRedundantStore)) {
            match.forEach(method.instructions::remove)
            assignments++
        }

        // remove redundant field assignments
        for (match in PUT_MATCHER.match(method).filter(::isRedundantPut)) {
            match.forEach(method.instructions::remove)
            assignments++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $opaquePredicates opaque predicates and $assignments redundant assignments" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val FLOW_OBSTRUCTOR_INITIALIZER_MATCHER = InsnMatcher.compile(
            """
            (GETSTATIC | ILOAD)
            IFEQ
            (((GETSTATIC ISTORE)? IINC ILOAD) | ((GETSTATIC | ILOAD) IFEQ ICONST GOTO ICONST))
            PUTSTATIC
        """
        )
        private val OPAQUE_PREDICATE_MATCHER = InsnMatcher.compile("(GETSTATIC | ILOAD) (IFEQ | IFNE)")
        private val PUT_MATCHER = InsnMatcher.compile("ICONST PUTSTATIC")
        private val STORE_MATCHER = InsnMatcher.compile("GETSTATIC ISTORE")
    }
}
