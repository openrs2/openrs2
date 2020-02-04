package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.createIntConstant
import dev.openrs2.asm.deleteSimpleExpression
import dev.openrs2.asm.intConstant
import dev.openrs2.asm.nextReal
import dev.openrs2.asm.pure
import dev.openrs2.asm.replaceSimpleExpression
import dev.openrs2.asm.stackMetadata
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.common.collect.DisjointSet
import dev.openrs2.deob.ArgRef
import dev.openrs2.deob.analysis.IntInterpreter
import dev.openrs2.deob.analysis.IntValue
import dev.openrs2.deob.analysis.SourcedIntValue
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer

class DummyArgTransformer : Transformer() {
    private data class ConditionalCall(
        val conditionVar: Int,
        val conditionOpcode: Int,
        val conditionValue: Int?,
        val method: DisjointSet.Partition<MemberRef>,
        val constArgs: List<Int?>
    )

    private enum class BranchResult {
        ALWAYS_TAKEN, NEVER_TAKEN, UNKNOWN;

        companion object {
            fun fromTakenNotTaken(taken: Int, notTaken: Int): BranchResult {
                require(taken != 0 || notTaken != 0)

                return when {
                    taken == 0 -> NEVER_TAKEN
                    notTaken == 0 -> ALWAYS_TAKEN
                    else -> UNKNOWN
                }
            }
        }
    }

    private val argValues: Multimap<ArgRef, SourcedIntValue> = HashMultimap.create()
    private val conditionalCalls: Multimap<DisjointSet.Partition<MemberRef>?, ConditionalCall> = HashMultimap.create()
    private val constArgs = mutableMapOf<DisjointSet.Partition<MemberRef>, Array<Set<Int>?>>()
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private var branchesSimplified = 0
    private var constantsInlined = 0

    private fun isMutuallyRecursiveDummy(
        method: DisjointSet.Partition<MemberRef>,
        arg: Int,
        source: DisjointSet.Partition<MemberRef>,
        value: Int
    ): Boolean {
        for (sourceToMethodCall in conditionalCalls[source]) {
            if (sourceToMethodCall.method != method) {
                continue
            }

            for (methodToSourceCall in conditionalCalls[method]) {
                if (methodToSourceCall.method != source || methodToSourceCall.conditionVar != arg) {
                    continue
                }

                var taken = if (methodToSourceCall.conditionValue != null) {
                    evaluateBinaryBranch(methodToSourceCall.conditionOpcode, value, methodToSourceCall.conditionValue)
                } else {
                    evaluateUnaryBranch(methodToSourceCall.conditionOpcode, value)
                }

                if (taken) {
                    continue
                }

                val constArg = methodToSourceCall.constArgs[sourceToMethodCall.conditionVar]!!

                taken = if (sourceToMethodCall.conditionValue != null) {
                    evaluateBinaryBranch(
                        sourceToMethodCall.conditionOpcode,
                        constArg,
                        sourceToMethodCall.conditionValue
                    )
                } else {
                    evaluateUnaryBranch(sourceToMethodCall.conditionOpcode, constArg)
                }

                if (taken) {
                    continue
                }

                return true
            }
        }

        return false
    }

    private fun union(
        method: DisjointSet.Partition<MemberRef>,
        arg: Int,
        intValues: Collection<SourcedIntValue>
    ): Set<Int>? {
        val set = mutableSetOf<Int>()

        for ((source, intValue) in intValues) {
            if (intValue !is IntValue.Constant) {
                return null
            }

            if (source == method) {
                continue
            }

            if (intValue.singleton != null) {
                if (isMutuallyRecursiveDummy(method, arg, source, intValue.singleton)) {
                    continue
                }
            }

            set.addAll(intValue.values)
        }

        return if (set.isEmpty()) {
            null
        } else {
            set
        }
    }

    override fun preTransform(classPath: ClassPath) {
        inheritedMethodSets = classPath.createInheritedMethodSets()
        branchesSimplified = 0
        constantsInlined = 0
    }

    override fun prePass(classPath: ClassPath) {
        argValues.clear()
        conditionalCalls.clear()
    }

    override fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        val parentMethod = inheritedMethodSets[MemberRef(clazz, method)]!!

        val stores = BooleanArray(method.maxLocals)
        for (insn in method.instructions) {
            if (insn is VarInsnNode && insn.opcode == Opcodes.ISTORE) {
                stores[insn.`var`] = true
            }
        }

        for (match in CONDITIONAL_CALL_MATCHER.match(method)) {
            var matchIndex = 0

            val load = match[matchIndex++] as VarInsnNode
            if (stores[load.`var`]) {
                continue
            }

            var callerSlots = Type.getArgumentsAndReturnSizes(method.desc) shr 2
            if (method.access and Opcodes.ACC_STATIC != 0) {
                callerSlots++
            }
            if (load.`var` >= callerSlots) {
                continue
            }

            val conditionValue: Int?
            var conditionOpcode = match[matchIndex].opcode
            if (conditionOpcode == Opcodes.IFEQ || conditionOpcode == Opcodes.IFNE) {
                conditionValue = null
                matchIndex++
            } else {
                conditionValue = match[matchIndex++].intConstant
                conditionOpcode = match[matchIndex++].opcode
            }

            val invoke = match[match.size - 1] as MethodInsnNode
            val invokeArgTypes = Type.getArgumentTypes(invoke.desc).size
            val constArgs = arrayOfNulls<Int>(invokeArgTypes)
            if (invoke.opcode != Opcodes.INVOKESTATIC) {
                matchIndex++
            }
            for (i in constArgs.indices) {
                val insn = match[matchIndex++]
                if (insn.opcode == Opcodes.ACONST_NULL) {
                    matchIndex++
                } else {
                    constArgs[i] = insn.intConstant
                }
            }

            val callee = inheritedMethodSets[MemberRef(invoke)] ?: continue
            conditionalCalls.put(
                parentMethod,
                ConditionalCall(load.`var`, conditionOpcode, conditionValue, callee, constArgs.asList())
            )
        }

        val parameters = constArgs[parentMethod]
        val analyzer = Analyzer(IntInterpreter(parameters))
        val frames = analyzer.analyze(clazz.name, method)

        var changed = false
        val alwaysTakenBranches = mutableListOf<JumpInsnNode>()
        val neverTakenBranches = mutableListOf<JumpInsnNode>()
        val constInsns = mutableMapOf<AbstractInsnNode, Int>()

        frame@ for ((i, frame) in frames.withIndex()) {
            if (frame == null) {
                continue
            }

            val stackSize = frame.stackSize

            val insn = method.instructions[i]
            when (insn.opcode) {
                Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                    val invoke = insn as MethodInsnNode
                    val invokedMethod = inheritedMethodSets[MemberRef(invoke)] ?: continue@frame
                    val args = Type.getArgumentTypes(invoke.desc).size

                    var k = 0
                    for (j in 0 until args) {
                        val arg = frame.getStack(stackSize - args + j)
                        argValues.put(ArgRef(invokedMethod, k), SourcedIntValue(parentMethod, arg))
                        k += arg.size
                    }
                }
                Opcodes.IFEQ, Opcodes.IFNE -> {
                    val value = frame.getStack(stackSize - 1)
                    if (value !is IntValue.Constant) {
                        continue@frame
                    }

                    val result = evaluateUnaryBranch(insn.opcode, value.values)
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (result) {
                        BranchResult.ALWAYS_TAKEN -> alwaysTakenBranches.add(insn as JumpInsnNode)
                        BranchResult.NEVER_TAKEN -> neverTakenBranches.add(insn as JumpInsnNode)
                    }
                }
                Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT,
                Opcodes.IF_ICMPLE -> {
                    val value1 = frame.getStack(stackSize - 2)
                    val value2 = frame.getStack(stackSize - 1)
                    if (value1 !is IntValue.Constant || value2 !is IntValue.Constant) {
                        continue@frame
                    }

                    val result = evaluateBinaryBranch(insn.opcode, value1.values, value2.values)
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (result) {
                        BranchResult.ALWAYS_TAKEN -> alwaysTakenBranches.add(insn as JumpInsnNode)
                        BranchResult.NEVER_TAKEN -> neverTakenBranches.add(insn as JumpInsnNode)
                    }
                }
                else -> {
                    if (!insn.pure || insn.intConstant != null) {
                        continue@frame
                    }

                    if (insn.stackMetadata().pushes != 1) {
                        continue@frame
                    }

                    val nextInsn = insn.nextReal ?: continue@frame
                    val nextInsnIndex = method.instructions.indexOf(nextInsn)
                    val nextFrame = frames[nextInsnIndex]

                    val value = nextFrame.getStack(nextFrame.stackSize - 1)
                    if (value is IntValue.Constant && value.singleton != null) {
                        constInsns[insn] = value.singleton
                    }
                }
            }
        }

        for (insn in alwaysTakenBranches) {
            if (method.instructions.replaceSimpleExpression(insn, JumpInsnNode(Opcodes.GOTO, insn.label))) {
                branchesSimplified++
                changed = true
            }
        }

        for (insn in neverTakenBranches) {
            if (method.instructions.deleteSimpleExpression(insn)) {
                branchesSimplified++
                changed = true
            }
        }

        for ((insn, value) in constInsns) {
            if (!method.instructions.contains(insn)) {
                continue
            }

            val replacement = createIntConstant(value)
            if (method.instructions.replaceSimpleExpression(insn, replacement)) {
                constantsInlined++
                changed = true
            }
        }

        return changed
    }

    override fun postPass(classPath: ClassPath) {
        for (method in inheritedMethodSets) {
            val args = (Type.getArgumentsAndReturnSizes(method.first().desc) shr 2) - 1

            var allUnknown = true
            val parameters = arrayOfNulls<Set<Int>?>(args)

            for (i in 0 until args) {
                val parameter = union(method, i, argValues[ArgRef(method, i)])
                if (parameter != null) {
                    allUnknown = false
                }
                parameters[i] = parameter
            }

            if (allUnknown) {
                constArgs.remove(method)
            } else {
                constArgs[method] = parameters
            }
        }
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Simplified $branchesSimplified dummy branches and inlined $constantsInlined constants" }
    }

    companion object {
        private val logger = InlineLogger()
        private val CONDITIONAL_CALL_MATCHER = InsnMatcher.compile(
            """
            ILOAD
            (IFEQ | IFNE |
                (ICONST | BIPUSH | SIPUSH | LDC)
                (IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE)
            )
            ALOAD?
            (ICONST | FCONST | DCONST | BIPUSH | SIPUSH | LDC | ACONST_NULL CHECKCAST)+
            (INVOKEVIRTUAL | INVOKESTATIC | INVOKEINTERFACE)
        """
        )

        private fun evaluateUnaryBranch(opcode: Int, values: Set<Int>): BranchResult {
            require(values.isNotEmpty())

            var taken = 0
            var notTaken = 0
            for (v in values) {
                if (evaluateUnaryBranch(opcode, v)) {
                    taken++
                } else {
                    notTaken++
                }
            }

            return BranchResult.fromTakenNotTaken(taken, notTaken)
        }

        private fun evaluateUnaryBranch(opcode: Int, value: Int): Boolean {
            return when (opcode) {
                Opcodes.IFEQ -> value == 0
                Opcodes.IFNE -> value != 0
                else -> throw IllegalArgumentException()
            }
        }

        private fun evaluateBinaryBranch(opcode: Int, values1: Set<Int>, values2: Set<Int>): BranchResult {
            require(values1.isNotEmpty() && values2.isNotEmpty())

            var taken = 0
            var notTaken = 0
            for (v1 in values1) {
                for (v2 in values2) {
                    if (evaluateBinaryBranch(opcode, v1, v2)) {
                        taken++
                    } else {
                        notTaken++
                    }
                }
            }

            return BranchResult.fromTakenNotTaken(taken, notTaken)
        }

        private fun evaluateBinaryBranch(opcode: Int, value1: Int, value2: Int): Boolean {
            return when (opcode) {
                Opcodes.IF_ICMPEQ -> value1 == value2
                Opcodes.IF_ICMPNE -> value1 != value2
                Opcodes.IF_ICMPLT -> value1 < value2
                Opcodes.IF_ICMPGE -> value1 >= value2
                Opcodes.IF_ICMPGT -> value1 > value2
                Opcodes.IF_ICMPLE -> value1 <= value2
                else -> throw IllegalArgumentException()
            }
        }
    }
}
