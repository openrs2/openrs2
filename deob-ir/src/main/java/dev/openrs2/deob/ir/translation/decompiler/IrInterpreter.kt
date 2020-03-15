package dev.openrs2.deob.ir.translation.decompiler

import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.intConstant
import dev.openrs2.asm.toPrettyString
import dev.openrs2.deob.ir.BinOp
import dev.openrs2.deob.ir.BinaryExpr
import dev.openrs2.deob.ir.CallExpr
import dev.openrs2.deob.ir.ConstExpr
import dev.openrs2.deob.ir.FieldStorage
import dev.openrs2.deob.ir.IndexExpr
import dev.openrs2.deob.ir.LocalVarStorage
import dev.openrs2.deob.ir.StaticStorage
import dev.openrs2.deob.ir.StmtVisitor
import dev.openrs2.deob.ir.Storage
import dev.openrs2.deob.ir.UnaryExpr
import dev.openrs2.deob.ir.UnaryOp
import dev.openrs2.deob.ir.VarExpr
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ASM7
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.CHECKCAST
import org.objectweb.asm.Opcodes.D2F
import org.objectweb.asm.Opcodes.D2L
import org.objectweb.asm.Opcodes.DADD
import org.objectweb.asm.Opcodes.DCMPG
import org.objectweb.asm.Opcodes.DDIV
import org.objectweb.asm.Opcodes.DMUL
import org.objectweb.asm.Opcodes.DNEG
import org.objectweb.asm.Opcodes.DREM
import org.objectweb.asm.Opcodes.DSUB
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.F2L
import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.I2B
import org.objectweb.asm.Opcodes.I2C
import org.objectweb.asm.Opcodes.I2D
import org.objectweb.asm.Opcodes.I2F
import org.objectweb.asm.Opcodes.I2L
import org.objectweb.asm.Opcodes.I2S
import org.objectweb.asm.Opcodes.IADD
import org.objectweb.asm.Opcodes.IALOAD
import org.objectweb.asm.Opcodes.ICONST_M1
import org.objectweb.asm.Opcodes.IDIV
import org.objectweb.asm.Opcodes.IFEQ
import org.objectweb.asm.Opcodes.IFGE
import org.objectweb.asm.Opcodes.IFGT
import org.objectweb.asm.Opcodes.IFLE
import org.objectweb.asm.Opcodes.IFLT
import org.objectweb.asm.Opcodes.IFNE
import org.objectweb.asm.Opcodes.IFNONNULL
import org.objectweb.asm.Opcodes.IFNULL
import org.objectweb.asm.Opcodes.IF_ACMPEQ
import org.objectweb.asm.Opcodes.IF_ACMPNE
import org.objectweb.asm.Opcodes.IF_ICMPEQ
import org.objectweb.asm.Opcodes.IF_ICMPGE
import org.objectweb.asm.Opcodes.IF_ICMPGT
import org.objectweb.asm.Opcodes.IF_ICMPLE
import org.objectweb.asm.Opcodes.IF_ICMPLT
import org.objectweb.asm.Opcodes.IF_ICMPNE
import org.objectweb.asm.Opcodes.ILOAD
import org.objectweb.asm.Opcodes.IMUL
import org.objectweb.asm.Opcodes.INEG
import org.objectweb.asm.Opcodes.INSTANCEOF
import org.objectweb.asm.Opcodes.IREM
import org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.Opcodes.ISHL
import org.objectweb.asm.Opcodes.ISHR
import org.objectweb.asm.Opcodes.ISTORE
import org.objectweb.asm.Opcodes.ISUB
import org.objectweb.asm.Opcodes.IUSHR
import org.objectweb.asm.Opcodes.IXOR
import org.objectweb.asm.Opcodes.L2D
import org.objectweb.asm.Opcodes.L2F
import org.objectweb.asm.Opcodes.LALOAD
import org.objectweb.asm.Opcodes.LDC
import org.objectweb.asm.Opcodes.LOOKUPSWITCH
import org.objectweb.asm.Opcodes.LSHL
import org.objectweb.asm.Opcodes.LSHR
import org.objectweb.asm.Opcodes.LUSHR
import org.objectweb.asm.Opcodes.LXOR
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.PUTFIELD
import org.objectweb.asm.Opcodes.SIPUSH
import org.objectweb.asm.Opcodes.SWAP
import org.objectweb.asm.Opcodes.TABLESWITCH
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Interpreter

class IrInterpreter : Interpreter<IrValue>(ASM7) {
    lateinit var visitor: StmtVisitor

    override fun newValue(type: Type?) = IrValue(null)

    override fun newEmptyValue(local: Int) = IrValue(VarExpr.local(local))

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type) =
        IrValue(VarExpr.local(local), type)

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out IrValue>): IrValue? = when (insn) {
        is MethodInsnNode -> handleMethodOperation(insn, values)
        is MultiANewArrayInsnNode -> handleNewMultiArrayOperation(insn, values)
        else -> error("Unrecognized nary operation instruction: $insn")
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: IrValue?,
        value2: IrValue?,
        value3: IrValue?
    ): IrValue {
        TODO("Not yet implemented")
    }

    override fun merge(value1: IrValue?, value2: IrValue?): IrValue? {
        return value1
    }

    override fun newReturnTypeValue(type: Type?): IrValue? {
        if (type == Type.VOID_TYPE) {
            return null
        }

        return IrValue(null, type)
    }

    /**
     * We ignore this, since we catch all of the return instructions (outwith RETURN) in [unaryOperation].
     */
    override fun returnOperation(insn: AbstractInsnNode, value: IrValue, expected: IrValue) {}

    /**
     * Interprets a bytecode instruction with a single argument. This method is called for the
     * following opcodes:
     *
     * <p>INEG, LNEG, FNEG, DNEG, IINC, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F,
     * I2B, I2C, I2S, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN,
     * FRETURN, DRETURN, ARETURN, PUTSTATIC, GETFIELD, NEWARRAY, ANEWARRAY, ARRAYLENGTH, ATHROW,
     * CHECKCAST, INSTANCEOF, MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL
     *
     * @param insn the bytecode instruction to be interpreted.
     * @param value the argument of the instruction to be interpreted.
     * @return the result of the interpretation of the given instruction.
     * @throws AnalyzerException if an error occurred during the interpretation.
     */
    override fun unaryOperation(insn: AbstractInsnNode, value: IrValue) = when (insn.opcode) {
        I2B -> handleCastOperation(insn, value, Type.BYTE_TYPE)
        I2C -> handleCastOperation(insn, value, Type.CHAR_TYPE)
        I2S -> handleCastOperation(insn, value, Type.SHORT_TYPE)
        I2D, L2D -> handleCastOperation(insn, value, Type.DOUBLE_TYPE)
        I2L, D2L, F2L -> handleCastOperation(insn, value, Type.LONG_TYPE)
        I2F, D2F, L2F -> handleCastOperation(insn, value, Type.FLOAT_TYPE)
        CHECKCAST -> handleCastOperation(insn, value, (insn as TypeInsnNode).desc.let(Type::getType))
        INSTANCEOF -> handleCastOperation(
            insn,
            value,
            (insn as TypeInsnNode).desc.let(Type::getType),
            instanceOf = true
        )
        GETFIELD -> handleVarLoadOperation(
            insn,
            FieldStorage(value.expr!!, MemberRef(insn as FieldInsnNode))
        )
        IFNULL, IFNONNULL -> handleConditionalOperation(
            insn as JumpInsnNode,
            value,
            IrValue(ConstExpr(null), value.type)
        )
        TABLESWITCH -> handleTableSwitchOperation(insn as TableSwitchInsnNode, value)
        LOOKUPSWITCH -> handleLookupSwitchOperation(insn as LookupSwitchInsnNode, value)
        in INEG..DNEG -> handleUnaryOperation(insn,value, UnaryOp.Negate)
        in IFEQ..IFLE -> handleConditionalOperation(insn as JumpInsnNode, value, IrValue(ConstExpr(0), Type.INT_TYPE))
        in IRETURN..ARETURN -> handleReturnOperation(insn, value)
        else -> error("Invalid instruction for unary expression: ${insn.toPrettyString()}")
    }

    private fun handleTableSwitchOperation(tableSwitchInsnNode: TableSwitchInsnNode, value: IrValue): IrValue? {
        TODO("Not yet implemented")
    }

    private fun handleLookupSwitchOperation(lookupSwitchInsnNode: LookupSwitchInsnNode, value: IrValue): IrValue? {
        TODO("Not yet implemented")
    }

    /**
     * Interprets a bytecode instruction with two arguments. This method is called for the following
     * opcodes:
     *
     * <p>IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IADD, LADD, FADD, DADD,
     * ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM,
     * ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, LCMP, FCMPL, FCMPG,
     * DCMPL, DCMPG, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
     * IF_ACMPNE, PUTFIELD
     *
     * @param insn the bytecode instruction to be interpreted.
     * @param value1 the first argument of the instruction to be interpreted.
     * @param value2 the second argument of the instruction to be interpreted.
     * @return the result of the interpretation of the given instruction.
     * @throws AnalyzerException if an error occurred during the interpretation.
     */
    override fun binaryOperation(insn: AbstractInsnNode, lhs: IrValue, rhs: IrValue) = when (insn.opcode) {
        PUTFIELD -> handleVarStoreOperation(insn, FieldStorage(lhs.expr!!, MemberRef(insn as FieldInsnNode)), rhs)
        in IALOAD..LALOAD -> handleArrayLoadOperation(insn, lhs, rhs)
        in IADD..DCMPG -> handleBinaryOperation(insn, lhs, rhs)
        in IF_ICMPEQ..IF_ACMPNE -> handleConditionalOperation(insn as JumpInsnNode, lhs, rhs)
        else -> error("Unhandled binary operation instruction: $insn")
    }

    private fun handleArrayLoadOperation(insn: AbstractInsnNode, index: IrValue, array: IrValue): IrValue? =
        IrValue(IndexExpr(array.expr!!, index.expr!!))

    override fun copyOperation(insn: AbstractInsnNode, value: IrValue) = when (insn.opcode) {
        in ISTORE..ASTORE -> {
            handleVarStoreOperation(insn, LocalVarStorage((insn as VarInsnNode).`var`), value)
            value
        }
        in ILOAD..ALOAD -> handleVarLoadOperation(insn, LocalVarStorage((insn as VarInsnNode).`var`))
        in DUP..SWAP -> {
            val local = LocalVarStorage()

            handleVarStoreOperation(insn, local, value)
            handleVarLoadOperation(insn, local)
        }
        else -> error("Invalid copy instruction: $insn")
    }

    override fun newOperation(insn: AbstractInsnNode) = when (insn.opcode) {
        NEW -> IrValue(null, (insn as TypeInsnNode).desc.let(Type::getType))
        ACONST_NULL -> handleConstantOperation(null)
        LDC -> handleConstantOperation((insn as LdcInsnNode).cst)
        GETSTATIC -> handleVarLoadOperation(insn, StaticStorage(MemberRef((insn as FieldInsnNode))))
        in ICONST_M1..SIPUSH -> handleConstantOperation(insn.intConstant!!)
        else -> error("Unhandled new operation instruction: $insn")
    }

    private fun handleConstantOperation(value: Any?) = IrValue(ConstExpr(value))

    fun handleBinaryOperation(insn: AbstractInsnNode, lhs: IrValue, rhs: IrValue): IrValue? {
        val op = when (insn.opcode) {
            in ISUB..DSUB -> BinOp.Subtract
            in IADD..DADD -> BinOp.Add
            in IMUL..DMUL -> BinOp.Multiply
            in IDIV..DDIV -> BinOp.Divide
            in IREM..DREM -> BinOp.Remainder
            in ISHL..LSHL -> BinOp.ShiftLeft
            in ISHR..LSHR -> BinOp.ShiftRight
            in IUSHR..LUSHR -> BinOp.UnsignedShiftRight
            in IXOR..LXOR -> BinOp.ExclusiveOr
            else -> error("Invalid instruction for binary expression: ${insn.toPrettyString()}")
        }

        return IrValue(BinaryExpr(lhs.expr!!, rhs.expr!!, op))
    }

    fun handleCastOperation(insn: AbstractInsnNode, value: IrValue, type: Type, instanceOf: Boolean = false): IrValue {
        val op = if (instanceOf) {
            UnaryOp.InstanceOf(type)
        } else {
            UnaryOp.Cast(type)
        }

        return IrValue(UnaryExpr(value.expr!!, op))
    }

    private fun handleConditionalOperation(insn: JumpInsnNode, lhs: IrValue, rhs: IrValue): IrValue? {
        val op = when (insn.opcode) {
            IFEQ, IF_ACMPEQ, IF_ICMPEQ, IFNULL -> BinOp.Equals
            IFNE, IF_ACMPNE, IF_ICMPNE, IFNONNULL -> BinOp.NotEquals
            IFLE, IF_ICMPLE -> BinOp.LessThanOrEquals
            IFLT, IF_ICMPLT -> BinOp.LessThan
            IFGE, IF_ICMPGE -> BinOp.GreaterThanOrEquals
            IFGT, IF_ICMPGT -> BinOp.GreaterThan
            else -> error("Not a valid conditional instruction: $insn")
        }

        visitor.visitIf(BinaryExpr(lhs.expr!!, rhs.expr!!, op))
        return null
    }

    fun handleMethodOperation(insn: MethodInsnNode, values: MutableList<out IrValue>): IrValue? {
        val method = MemberRef(insn)
        val returnType = Type.getReturnType(insn.desc)
        val instance = when (insn.opcode) {
            Opcodes.INVOKESTATIC -> null
            else -> values.removeAt(0).expr ?: error("No expression found for isntance slot")
        }

        val call = CallExpr(instance, method, values.map { it.expr!! })

        return when (returnType) {
            Type.VOID_TYPE -> {
                visitor.visitCall(call)
                null
            }
            else -> IrValue(call, returnType)
        }
    }

    fun handleNewMultiArrayOperation(insn: MultiANewArrayInsnNode, values: MutableList<out IrValue>): IrValue? {
        return null
    }

    fun handleReturnOperation(insn: AbstractInsnNode, value: IrValue?): IrValue? {
        visitor.visitReturn(value?.expr)
        return null
    }

    fun handleUnaryOperation(insn: AbstractInsnNode, value: IrValue, operator: UnaryOp): IrValue {
        return IrValue(UnaryExpr(value.expr!!, operator))
    }

    fun handleVarLoadOperation(insn: AbstractInsnNode, storage: Storage): IrValue? {
        return IrValue(VarExpr(storage))
    }

    fun handleVarStoreOperation(insn: AbstractInsnNode, storage: Storage, value: IrValue): IrValue? {
        visitor.visitAssignmen(VarExpr(storage), value.expr!!)
        return null
    }
}
