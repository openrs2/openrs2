package org.openrs2.asm.packclass

import io.netty.buffer.ByteBuf
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.nextReal
import org.openrs2.buffer.Arena
import org.openrs2.buffer.readUnsignedShortSmart
import org.openrs2.buffer.readVarInt
import org.openrs2.buffer.writeUnsignedShortSmart
import org.openrs2.buffer.writeVarInt

public object PackClass {
    public const val CLASS_GROUP: Int = 0
    public const val CONSTANT_POOL_GROUP: Int = 1
    public const val CONSTANT_POOL_FILE: Int = 0

    private const val TRAILER_LEN = 6

    private val SUPPORTED_VERSIONS = mutableSetOf(
        Opcodes.V1_1,
        Opcodes.V1_2,
        Opcodes.V1_3,
        Opcodes.V1_4,
        Opcodes.V1_5,
        Opcodes.V1_6
    )

    private const val INT_DESCRIPTOR = "I"
    private const val LONG_DESCRIPTOR = "J"
    private const val FLOAT_DESCRIPTOR = "F"
    private const val DOUBLE_DESCRIPTOR = "D"
    private const val STRING_DESCRIPTOR = "Ljava/lang/String;"

    // opcodes
    private const val WIDE = 0xC4
    private const val LDC_INT = Opcodes.LDC
    private const val LDC_FLOAT = 0x13
    private const val LDC_LONG = 0x14
    private const val LDC_STRING = Opcodes.INVOKEDYNAMIC
    private const val LDC_DOUBLE = 0xCA
    private const val LDC_CLASS = 0xCB
    private const val ILOAD_0 = 0x1A
    private const val ILOAD_3 = 0x1D
    private const val LLOAD_0 = 0x1E
    private const val LLOAD_3 = 0x21
    private const val FLOAD_0 = 0x22
    private const val FLOAD_3 = 0x25
    private const val DLOAD_0 = 0x26
    private const val DLOAD_3 = 0x29
    private const val ALOAD_0 = 0x2A
    private const val ALOAD_3 = 0x2D
    private const val ISTORE_0 = 0x3B
    private const val ISTORE_3 = 0x3E
    private const val LSTORE_0 = 0x3F
    private const val LSTORE_3 = 0x42
    private const val FSTORE_0 = 0x43
    private const val FSTORE_3 = 0x46
    private const val DSTORE_0 = 0x47
    private const val DSTORE_3 = 0x4A
    private const val ASTORE_0 = 0x4B
    private const val ASTORE_3 = 0x4E
    private const val GOTO_W = 0xC8
    private const val JSR_W = 0xC9
    private const val EOF = 0xCC

    // opcode flags
    private const val OPCODE_INVALID = 0x1
    private const val OPCODE_LOCAL_VAR = 0x2
    private const val OPCODE_CONSTANT = 0x4
    private const val OPCODE_WIDE_CONSTANT = 0x10
    private const val OPCODE_CLASS = 0x20
    private const val OPCODE_FIELD_REF = 0x40
    private const val OPCODE_METHOD_REF = 0x80
    private const val OPCODE_BRANCH = 0x100
    private const val OPCODE_WIDE_BRANCH = 0x200
    private const val OPCODE_WIDE_LOCAL_VAR = 0x400

    private val OPCODE_FLAGS = intArrayOf(
        0, // NOP
        0, // ACONST_NULL
        0, // ICONST_M1
        0, // ICONST_0
        0, // ICONST_1
        0, // ICONST_2
        0, // ICONST_3
        0, // ICONST_4
        0, // ICONST_5
        0, // LCONST_0
        0, // LCONST_1
        0, // FCONST_0
        0, // FCONST_1
        0, // FCONST_2
        0, // DCONST_0
        0, // DCONST_1
        0, // BIPUSH
        0, // SIPUSH
        OPCODE_CONSTANT, // LDC (used as LDC_INTEGER)
        OPCODE_CONSTANT, // LDC_W (used as LDC_FLOAT)
        OPCODE_WIDE_CONSTANT, // LDC2_W (used as LDC_STRING)
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // ILOAD
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // LLOAD
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // FLOAD
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // DLOAD
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // ALOAD
        0, // ILOAD_0
        0, // ILOAD_1
        0, // ILOAD_2
        0, // ILOAD_3
        0, // LLOAD_0
        0, // LLOAD_1
        0, // LLOAD_2
        0, // LLOAD_3
        0, // FLOAD_0
        0, // FLOAD_1
        0, // FLOAD_2
        0, // FLOAD_3
        0, // DLOAD_0
        0, // DLOAD_1
        0, // DLOAD_2
        0, // DLOAD_3
        0, // ALOAD_0
        0, // ALOAD_1
        0, // ALOAD_2
        0, // ALOAD_3
        0, // IALOAD
        0, // LALOAD
        0, // FALOAD
        0, // DALOAD
        0, // AALOAD
        0, // BALOAD
        0, // CALOAD
        0, // SALOAD
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // ISTORE
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // LSTORE
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // FSTORE
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // DSTORE
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // ASTORE
        0, // ISTORE_0
        0, // ISTORE_1
        0, // ISTORE_2
        0, // ISTORE_3
        0, // LSTORE_0
        0, // LSTORE_1
        0, // LSTORE_2
        0, // LSTORE_3
        0, // FSTORE_0
        0, // FSTORE_1
        0, // FSTORE_2
        0, // FSTORE_3
        0, // DSTORE_0
        0, // DSTORE_1
        0, // DSTORE_2
        0, // DSTORE_3
        0, // ASTORE_0
        0, // ASTORE_1
        0, // ASTORE_2
        0, // ASTORE_3
        0, // IASTORE
        0, // LASTORE
        0, // FASTORE
        0, // DASTORE
        0, // AASTORE
        0, // BASTORE
        0, // CASTORE
        0, // SASTORE
        0, // POP
        0, // POP2
        0, // DUP
        0, // DUP_X1
        0, // DUP_X2
        0, // DUP2
        0, // DUP2_X1
        0, // DUP2_X2
        0, // SWAP
        0, // IADD
        0, // LADD
        0, // FADD
        0, // DADD
        0, // ISUB
        0, // LSUB
        0, // FSUB
        0, // DSUB
        0, // IMUL
        0, // LMUL
        0, // FMUL
        0, // DMUL
        0, // IDIV
        0, // LDIV
        0, // FDIV
        0, // DDIV
        0, // IREM
        0, // LREM
        0, // FREM
        0, // DREM
        0, // INEG
        0, // LNEG
        0, // FNEG
        0, // DNEG
        0, // ISHL
        0, // LSHL
        0, // ISHR
        0, // LSHR
        0, // IUSHR
        0, // LUSHR
        0, // IAND
        0, // LAND
        0, // IOR
        0, // LOR
        0, // IXOR
        0, // LXOR
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // IINC
        0, // I2L
        0, // I2F
        0, // I2D
        0, // L2I
        0, // L2F
        0, // L2D
        0, // F2I
        0, // F2L
        0, // F2D
        0, // D2I
        0, // D2L
        0, // D2F
        0, // I2B
        0, // I2C
        0, // I2S
        0, // LCMP
        0, // FCMPL
        0, // FCMPG
        0, // DCMPL
        0, // DCMPG
        OPCODE_BRANCH, // IFEQ
        OPCODE_BRANCH, // IFNE
        OPCODE_BRANCH, // IFLT
        OPCODE_BRANCH, // IFGE
        OPCODE_BRANCH, // IFGT
        OPCODE_BRANCH, // IFLE
        OPCODE_BRANCH, // IF_ICMPEQ
        OPCODE_BRANCH, // IF_ICMPNE
        OPCODE_BRANCH, // IF_ICMPLT
        OPCODE_BRANCH, // IF_ICMPGE
        OPCODE_BRANCH, // IF_ICMPGT
        OPCODE_BRANCH, // IF_ICMPLE
        OPCODE_BRANCH, // IF_ACMPEQ
        OPCODE_BRANCH, // IF_ACMPNE
        OPCODE_BRANCH, // GOTO
        OPCODE_BRANCH, // JSR
        OPCODE_LOCAL_VAR or OPCODE_WIDE_LOCAL_VAR, // RET
        0, // TABLESWITCH
        0, // LOOKUPSWITCH
        0, // IRETURN
        0, // LRETURN
        0, // FRETURN
        0, // DRETURN
        0, // ARETURN
        0, // RETURN
        OPCODE_FIELD_REF, // GETSTATIC
        OPCODE_FIELD_REF, // PUTSTATIC
        OPCODE_FIELD_REF, // GETFIELD
        OPCODE_FIELD_REF, // PUTFIELD
        OPCODE_METHOD_REF, // INVOKEVIRTUAL
        OPCODE_METHOD_REF, // INVOKESPECIAL
        OPCODE_METHOD_REF, // INVOKESTATIC
        0, // INVOKEINTERFACE
        OPCODE_CONSTANT, // INVOKEDYNAMIC (used as LDC_STRING)
        OPCODE_CLASS, // NEW
        0, // NEWARRAY
        OPCODE_CLASS, // ANEWARRAY
        0, // ARRAYLENGTH
        0, // ATHROW
        OPCODE_CLASS, // CHECKCAST
        OPCODE_CLASS, // INSTANCEOF
        0, // MONITORENTER
        0, // MONITOREXIT
        0, // WIDE
        OPCODE_CLASS, // MULTIANEWARRAY
        OPCODE_BRANCH, // IFNULL
        OPCODE_BRANCH, // IFNONNULL
        OPCODE_WIDE_BRANCH, // GOTO_W
        OPCODE_WIDE_BRANCH, // JSR_W
        OPCODE_WIDE_CONSTANT, // BREAKPOINT (used as LDC_DOUBLE)
        OPCODE_CONSTANT, // (unused, used as LDC_CLASS)
        OPCODE_INVALID, // (unused, used as EOF)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // (unused)
        OPCODE_INVALID, // IMPDEP1
        OPCODE_INVALID // IMPDEP2
    )

    public fun read(buf: ByteBuf, constantPool: ConstantPool): ClassNode {
        val clazz = ClassNode()

        // read trailer
        buf.markReaderIndex()
        buf.readerIndex(buf.writerIndex() - TRAILER_LEN)

        val interfaceCount = buf.readUnsignedShort()
        val fieldCount = buf.readUnsignedShort()
        val methodCount = buf.readUnsignedShort()

        buf.resetReaderIndex()

        // read attributes
        val fieldAttributeCounts = readAttributeCounts(buf, fieldCount)
        val methodAttributeCounts = readAttributeCounts(buf, methodCount)

        val fieldAttributes = readAttributes(buf, constantPool, fieldAttributeCounts)
        val methodAttributes = readAttributes(buf, constantPool, methodAttributeCounts)

        // read method metadata
        val exceptionCounts = readExceptionCounts(buf, methodAttributes)
        val tryCatchCounts = readTryCatchCounts(buf, methodAttributes)
        val maxStacks = readMaxs(buf, methodAttributes)
        val maxLocals = readMaxs(buf, methodAttributes)
        val lineStartPcs = readLinePcs(buf, methodAttributes)
        val lineNumbers = readLineNumbers(buf, methodAttributes, lineStartPcs)

        // read access flags
        clazz.access = buf.readUnsignedByte().toInt() shl 8

        val fieldAccess = readAccessFlagsMsb(buf, fieldCount)
        val methodAccess = readAccessFlagsMsb(buf, methodCount)

        clazz.access = clazz.access or buf.readUnsignedByte().toInt()

        readAccessFlagsLsb(buf, fieldAccess)
        readAccessFlagsLsb(buf, methodAccess)

        // read source file
        clazz.sourceFile = constantPool.readOptionalString(buf)

        // read exception types
        val exceptions = readExceptions(buf, constantPool, methodAttributes, exceptionCounts)
        val tryCatchTypes = readTryCatchTypes(buf, constantPool, methodAttributes, tryCatchCounts)

        // read class and superclass name
        clazz.name = constantPool.readString(buf)
        clazz.superName = constantPool.readString(buf)

        // read interface names
        readInterfaces(buf, constantPool, interfaceCount, clazz)

        // read field and method names and types
        val fieldNamesAndTypes = readFieldNamesAndTypes(buf, constantPool, fieldCount)
        val methodNamesAndTypes = readMethodNamesAndTypes(buf, constantPool, methodCount)

        // read version
        val minor = buf.readUnsignedShort()
        val major = buf.readUnsignedShort()
        clazz.version = (minor shl 16) or major

        require(clazz.version in SUPPORTED_VERSIONS) {
            "Unsupported class version $major.$minor"
        }

        // read constant field values
        val fieldConstants = readConstants(buf, constantPool, fieldAttributes, fieldNamesAndTypes)

        // read try/catch blocks
        val tryCatchStartPcs = readTryCatchStartPcs(buf, methodAttributes, tryCatchCounts)
        val tryCatchEndPcsToHandlerPcs = readTryCatchEndPcsToHandlerPcs(buf, methodAttributes, tryCatchCounts)
        val tryCatchReverseHandlerPcs = readTryCatchReverseHandlerPcs(buf, methodAttributes, tryCatchCounts)

        // calculate operand buffer sizes
        buf.markReaderIndex()

        var newArrayLen = 0
        var localVarLen = 0
        var wideLocalVarLen = 0
        var sipushAndSwitchLen = 0
        var constantLen = 0
        var wideConstantLen = 0
        var classLen = 0
        var fieldRefLen = 0
        var methodRefLen = 0
        var interfaceMethodRefLen = 0
        var branchLen = 0
        var bipushLen = 0
        var wideIincLen = 0
        var iincLen = 0
        var multiNewArrayLen = 0

        val methodOpcodes = IntArray(methodAttributes.size) { i ->
            if (methodAttributes[i].contains(ConstantPool.CODE)) {
                var pc = 0

                while (true) {
                    val opcode = buf.readUnsignedByte().toInt()
                    if (opcode == EOF) {
                        break
                    }

                    val flags = OPCODE_FLAGS[opcode]
                    if ((flags and OPCODE_INVALID) != 0) {
                        throw IllegalArgumentException("Invalid opcode: $opcode")
                    }

                    if ((flags and OPCODE_LOCAL_VAR) != 0) {
                        localVarLen++
                    }

                    if ((flags and OPCODE_CONSTANT) != 0) {
                        constantLen += 2
                    }

                    if ((flags and OPCODE_WIDE_CONSTANT) != 0) {
                        wideConstantLen += 2
                    }

                    if ((flags and OPCODE_CLASS) != 0) {
                        classLen += 2
                    }

                    if ((flags and OPCODE_FIELD_REF) != 0) {
                        fieldRefLen += 2
                    }

                    if ((flags and OPCODE_METHOD_REF) != 0) {
                        methodRefLen += 2
                    }

                    if ((flags and OPCODE_BRANCH) != 0) {
                        branchLen += 2
                    }

                    if ((flags and OPCODE_WIDE_BRANCH) != 0) {
                        branchLen += 4
                    }

                    when (opcode) {
                        Opcodes.BIPUSH -> bipushLen++
                        Opcodes.SIPUSH -> sipushAndSwitchLen += 2
                        Opcodes.IINC -> iincLen++
                        Opcodes.TABLESWITCH -> {
                            val cases = buf.readVarInt()
                            branchLen += (cases + 2) * 4
                            sipushAndSwitchLen += 4
                        }

                        Opcodes.LOOKUPSWITCH -> {
                            val cases = buf.readVarInt()
                            branchLen += (cases + 1) * 4
                            sipushAndSwitchLen += cases * 4
                        }

                        Opcodes.INVOKEINTERFACE -> interfaceMethodRefLen += 2
                        Opcodes.NEWARRAY -> newArrayLen++
                        Opcodes.MULTIANEWARRAY -> multiNewArrayLen++
                        WIDE -> {
                            val wideOpcode = buf.readUnsignedByte().toInt()
                            val wideFlags = OPCODE_FLAGS[opcode]

                            if ((wideFlags and OPCODE_WIDE_LOCAL_VAR) != 0) {
                                throw IllegalArgumentException("Invalid wide opcode: $wideOpcode")
                            }

                            wideLocalVarLen += 2

                            if (wideOpcode == Opcodes.IINC) {
                                wideIincLen += 2
                            }
                        }
                    }

                    pc++
                }

                pc
            } else {
                0
            }
        }

        // read operand buffers
        val newArrayBuf = buf.readSlice(newArrayLen)
        val localVarBuf = buf.readSlice(localVarLen)
        val wideLocalVarBuf = buf.readSlice(wideLocalVarLen)
        val sipushAndSwitchBuf = buf.readSlice(sipushAndSwitchLen)
        val constantBuf = buf.readSlice(constantLen)
        val wideConstantBuf = buf.readSlice(wideConstantLen)
        val classBuf = buf.readSlice(classLen)
        val fieldRefBuf = buf.readSlice(fieldRefLen)
        val methodRefBuf = buf.readSlice(methodRefLen)
        val interfaceMethodRefBuf = buf.readSlice(interfaceMethodRefLen)
        val branchBuf = buf.readSlice(branchLen)
        val bipushBuf = buf.readSlice(bipushLen)
        val wideIincBuf = buf.readSlice(wideIincLen)
        val iincBuf = buf.readSlice(iincLen)
        val multiNewArrayBuf = buf.readSlice(multiNewArrayLen)

        buf.resetReaderIndex()

        // create fields
        for (i in 0 until fieldCount) {
            var access = fieldAccess[i]

            for (attribute in fieldAttributes[i]) {
                if (attribute == ConstantPool.SYNTHETIC) {
                    access = access or Opcodes.ACC_SYNTHETIC
                } else {
                    throw IllegalArgumentException("Unsupported field attribute: $attribute")
                }
            }

            val nameAndType = fieldNamesAndTypes[i]
            val name = nameAndType.name
            val descriptor = nameAndType.descriptor

            val constant = fieldConstants[i]

            clazz.fields.add(FieldNode(access, name, descriptor, null, constant))
        }

        // create methods
        for (i in 0 until methodCount) {
            var access = methodAccess[i]
            var code = false

            for (attribute in methodAttributes[i]) {
                when (attribute) {
                    ConstantPool.SYNTHETIC -> access = access or Opcodes.ACC_SYNTHETIC
                    ConstantPool.CODE -> code = true
                    ConstantPool.EXCEPTIONS -> Unit
                    else -> throw IllegalArgumentException("Unsupported method attribute: $attribute")
                }
            }

            val nameAndType = methodNamesAndTypes[i]
            val name = nameAndType.name
            val descriptor = nameAndType.descriptor

            val method = MethodNode(access, name, descriptor, null, exceptions[i])

            if (code) {
                method.maxLocals = maxLocals[i]
                method.maxStack = maxStacks[i]

                val labels = Array(methodOpcodes[i]) {
                    val label = LabelNode()
                    method.instructions.add(label)
                    label
                }

                for (j in 0 until tryCatchCounts[i]) {
                    val startPc = tryCatchStartPcs[i]!![j]
                    val handlerPc = labels.size - tryCatchReverseHandlerPcs[i]!![j]
                    val endPc = handlerPc - tryCatchEndPcsToHandlerPcs[i]!![j]

                    val start = labels[startPc]
                    val end = labels[endPc]
                    val handler = labels[handlerPc]

                    val type = tryCatchTypes[i]!![j]

                    method.tryCatchBlocks.add(TryCatchBlockNode(start, end, handler, type))
                }

                val numbers = lineNumbers[i]
                if (numbers != null) {
                    val startPcs = lineStartPcs[i]!!

                    for ((j, number) in numbers.withIndex()) {
                        val startPc = startPcs[j]
                        val label = labels[startPc]
                        method.instructions.insert(label, LineNumberNode(number, label))
                    }
                }

                var pc = 0

                while (true) {
                    val opcode = buf.readUnsignedByte().toInt()
                    if (opcode == EOF) {
                        break
                    }

                    val flags = OPCODE_FLAGS[opcode]
                    if ((flags and OPCODE_INVALID) != 0) {
                        throw IllegalArgumentException("Invalid opcode: $opcode")
                    }

                    val insn = if ((flags and OPCODE_LOCAL_VAR) != 0 && opcode != Opcodes.IINC) {
                        VarInsnNode(opcode, localVarBuf.readUnsignedByte().toInt())
                    } else if ((flags and OPCODE_CONSTANT) != 0) {
                        val value: Any = when (opcode) {
                            LDC_INT -> constantPool.readInt(constantBuf)
                            LDC_FLOAT -> constantPool.readFloat(constantBuf)
                            LDC_STRING -> constantPool.readString(constantBuf)
                            LDC_CLASS -> Type.getObjectType(constantPool.readString(constantBuf))
                            else -> throw IllegalArgumentException("Invalid constant opcode: $opcode")
                        }
                        LdcInsnNode(value)
                    } else if ((flags and OPCODE_WIDE_CONSTANT) != 0) {
                        val value: Any = when (opcode) {
                            LDC_LONG -> constantPool.readLong(wideConstantBuf)
                            LDC_DOUBLE -> constantPool.readDouble(wideConstantBuf)
                            else -> IllegalArgumentException("Invalid wide constant opcode: $opcode")
                        }
                        LdcInsnNode(value)
                    } else if ((flags and OPCODE_CLASS) != 0 && opcode != Opcodes.MULTIANEWARRAY) {
                        TypeInsnNode(opcode, constantPool.readString(classBuf))
                    } else if ((flags and OPCODE_FIELD_REF) != 0) {
                        val fieldRef = constantPool.readFieldRef(fieldRefBuf)
                        FieldInsnNode(opcode, fieldRef.clazz, fieldRef.name, fieldRef.descriptor)
                    } else if ((flags and OPCODE_METHOD_REF) != 0) {
                        val methodRef = constantPool.readMethodRef(methodRefBuf)
                        MethodInsnNode(opcode, methodRef.clazz, methodRef.name, methodRef.descriptor)
                    } else if ((flags and OPCODE_BRANCH) != 0) {
                        val targetPc = pc + branchBuf.readShort().toInt()
                        JumpInsnNode(opcode, labels[targetPc])
                    } else if ((flags and OPCODE_WIDE_BRANCH) != 0) {
                        val wideOpcode = when (opcode) {
                            GOTO_W -> Opcodes.GOTO
                            JSR_W -> Opcodes.JSR
                            else -> throw IllegalArgumentException("Invalid wide branch opcode: $opcode")
                        }
                        val targetPc = pc + branchBuf.readInt()
                        JumpInsnNode(wideOpcode, labels[targetPc])
                    } else if (opcode == Opcodes.BIPUSH) {
                        IntInsnNode(opcode, bipushBuf.readByte().toInt())
                    } else if (opcode == Opcodes.SIPUSH) {
                        IntInsnNode(opcode, sipushAndSwitchBuf.readShort().toInt())
                    } else if (opcode == Opcodes.IINC) {
                        IincInsnNode(localVarBuf.readUnsignedByte().toInt(), iincBuf.readByte().toInt())
                    } else if (opcode == Opcodes.TABLESWITCH) {
                        val defaultPc = pc + branchBuf.readInt()
                        val defaultLabel = labels[defaultPc]

                        val min = sipushAndSwitchBuf.readInt()
                        val max = min + buf.readVarInt()

                        val targetLabels = Array(max - min + 1) {
                            val targetPc = pc + branchBuf.readInt()
                            labels[targetPc]
                        }

                        TableSwitchInsnNode(min, max, defaultLabel, *targetLabels)
                    } else if (opcode == Opcodes.LOOKUPSWITCH) {
                        val defaultPc = pc + branchBuf.readInt()
                        val defaultLabel = labels[defaultPc]

                        val cases = buf.readVarInt()

                        val keys = IntArray(cases)
                        var key = 0
                        val targetLabels = Array(cases) { j ->
                            key += sipushAndSwitchBuf.readInt()
                            keys[j] = key

                            val targetPc = pc + branchBuf.readInt()
                            labels[targetPc]
                        }

                        LookupSwitchInsnNode(defaultLabel, keys, targetLabels)
                    } else if (opcode == Opcodes.INVOKEINTERFACE) {
                        val methodRef = constantPool.readInterfaceMethodRef(interfaceMethodRefBuf)
                        MethodInsnNode(opcode, methodRef.clazz, methodRef.name, methodRef.descriptor)
                    } else if (opcode == Opcodes.NEWARRAY) {
                        IntInsnNode(opcode, newArrayBuf.readUnsignedByte().toInt())
                    } else if (opcode == Opcodes.MULTIANEWARRAY) {
                        val arrayDescriptor = constantPool.readString(classBuf)
                        val dimensions = multiNewArrayBuf.readUnsignedByte().toInt()
                        MultiANewArrayInsnNode(arrayDescriptor, dimensions)
                    } else if (opcode == WIDE) {
                        val wideOpcode = buf.readUnsignedByte().toInt()
                        val wideFlags = OPCODE_FLAGS[wideOpcode]

                        if ((wideFlags and OPCODE_WIDE_LOCAL_VAR) == 0) {
                            throw IllegalArgumentException("Invalid wide opcode: $wideOpcode")
                        }

                        val variable = wideLocalVarBuf.readUnsignedShort()
                        if (wideOpcode == Opcodes.IINC) {
                            IincInsnNode(variable, wideIincBuf.readShort().toInt())
                        } else {
                            VarInsnNode(wideOpcode, variable)
                        }
                    } else if (opcode in ILOAD_0..ILOAD_3) {
                        VarInsnNode(Opcodes.ILOAD, opcode - ILOAD_0)
                    } else if (opcode in LLOAD_0..LLOAD_3) {
                        VarInsnNode(Opcodes.LLOAD, opcode - LLOAD_0)
                    } else if (opcode in FLOAD_0..FLOAD_3) {
                        VarInsnNode(Opcodes.FLOAD, opcode - FLOAD_0)
                    } else if (opcode in DLOAD_0..DLOAD_3) {
                        VarInsnNode(Opcodes.DLOAD, opcode - DLOAD_0)
                    } else if (opcode in ALOAD_0..ALOAD_3) {
                        VarInsnNode(Opcodes.ALOAD, opcode - ALOAD_0)
                    } else if (opcode in ISTORE_0..ISTORE_3) {
                        VarInsnNode(Opcodes.ISTORE, opcode - ISTORE_0)
                    } else if (opcode in LSTORE_0..LSTORE_3) {
                        VarInsnNode(Opcodes.LSTORE, opcode - LSTORE_0)
                    } else if (opcode in FSTORE_0..FSTORE_3) {
                        VarInsnNode(Opcodes.FSTORE, opcode - FSTORE_0)
                    } else if (opcode in DSTORE_0..DSTORE_3) {
                        VarInsnNode(Opcodes.DSTORE, opcode - DSTORE_0)
                    } else if (opcode in ASTORE_0..ASTORE_3) {
                        VarInsnNode(Opcodes.ASTORE, opcode - ASTORE_0)
                    } else {
                        InsnNode(opcode)
                    }

                    method.instructions.insert(labels[pc++], insn)
                }
            }

            clazz.methods.add(method)
        }

        buf.skipBytes(newArrayLen)
        buf.skipBytes(localVarLen)
        buf.skipBytes(wideLocalVarLen)
        buf.skipBytes(sipushAndSwitchLen)
        buf.skipBytes(constantLen)
        buf.skipBytes(wideConstantLen)
        buf.skipBytes(classLen)
        buf.skipBytes(fieldRefLen)
        buf.skipBytes(methodRefLen)
        buf.skipBytes(interfaceMethodRefLen)
        buf.skipBytes(branchLen)
        buf.skipBytes(bipushLen)
        buf.skipBytes(wideIincLen)
        buf.skipBytes(iincLen)
        buf.skipBytes(multiNewArrayLen)
        buf.skipBytes(TRAILER_LEN)

        return clazz
    }

    private fun readAttributeCounts(buf: ByteBuf, count: Int): IntArray {
        return IntArray(count) {
            buf.readUnsignedByte().toInt()
        }
    }

    private fun readAttributes(buf: ByteBuf, constantPool: ConstantPool, counts: IntArray): Array<Array<String>> {
        return Array(counts.size) { i ->
            Array(counts[i]) {
                constantPool.readString(buf)
            }
        }
    }

    private fun readExceptionCounts(buf: ByteBuf, attributes: Array<Array<String>>): IntArray {
        return IntArray(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.EXCEPTIONS)) {
                buf.readUnsignedByte().toInt()
            } else {
                0
            }
        }
    }

    private fun readTryCatchCounts(buf: ByteBuf, attributes: Array<Array<String>>): IntArray {
        return IntArray(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                /*
                 * XXX(gpe): in older versions of packclass this is an unsigned
                 * byte. Are there any class files using the older format with
                 * 128 or more try/catch blocks per method?
                 */
                buf.readUnsignedShortSmart()
            } else {
                0
            }
        }
    }

    private fun readMaxs(buf: ByteBuf, attributes: Array<Array<String>>): IntArray {
        return IntArray(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                buf.readUnsignedByte().toInt()
            } else {
                0
            }
        }
    }

    private fun readLinePcs(buf: ByteBuf, attributes: Array<Array<String>>): Array<IntArray?> {
        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                buf.markReaderIndex()

                var lines = 0
                while (buf.readUnsignedShortSmart() != 0) {
                    lines++
                }

                buf.resetReaderIndex()

                var pc = -1
                val pcs = IntArray(lines) {
                    pc += buf.readUnsignedShortSmart()
                    pc
                }

                check(buf.readUnsignedShortSmart() == 0)

                pcs
            } else {
                null
            }
        }
    }

    private fun readLineNumbers(
        buf: ByteBuf,
        attributes: Array<Array<String>>,
        pcs: Array<IntArray?>
    ): Array<IntArray?> {
        require(attributes.size == pcs.size)

        var line = 0

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                IntArray(pcs[i]!!.size) {
                    line += buf.readUnsignedShort()
                    line and 0xFFFF
                }
            } else {
                null
            }
        }
    }

    private fun readAccessFlagsMsb(buf: ByteBuf, count: Int): IntArray {
        return IntArray(count) {
            buf.readUnsignedByte().toInt() shl 8
        }
    }

    private fun readAccessFlagsLsb(buf: ByteBuf, access: IntArray) {
        for (i in access.indices) {
            access[i] = access[i] or buf.readUnsignedByte().toInt()
        }
    }

    private fun readExceptions(
        buf: ByteBuf,
        constantPool: ConstantPool,
        attributes: Array<Array<String>>,
        counts: IntArray
    ): Array<Array<String>?> {
        require(attributes.size == counts.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.EXCEPTIONS)) {
                Array(counts[i]) {
                    constantPool.readString(buf)
                }
            } else {
                null
            }
        }
    }

    private fun readTryCatchTypes(
        buf: ByteBuf,
        constantPool: ConstantPool,
        attributes: Array<Array<String>>,
        counts: IntArray
    ): Array<Array<String?>?> {
        require(attributes.size == counts.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                Array(counts[i]) {
                    constantPool.readOptionalString(buf)
                }
            } else {
                null
            }
        }
    }

    private fun readInterfaces(buf: ByteBuf, constantPool: ConstantPool, interfaceCount: Int, clazz: ClassNode) {
        for (i in 0 until interfaceCount) {
            clazz.interfaces.add(constantPool.readString(buf))
        }
    }

    private fun readFieldNamesAndTypes(buf: ByteBuf, constantPool: ConstantPool, count: Int): Array<NameAndType> {
        return Array(count) {
            constantPool.readFieldNameAndType(buf)
        }
    }

    private fun readMethodNamesAndTypes(buf: ByteBuf, constantPool: ConstantPool, count: Int): Array<NameAndType> {
        return Array(count) {
            constantPool.readMethodNameAndType(buf)
        }
    }

    private fun readConstants(
        buf: ByteBuf,
        constantPool: ConstantPool,
        attributes: Array<Array<String>>,
        namesAndTypes: Array<NameAndType>
    ): Array<Any?> {
        require(attributes.size == namesAndTypes.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CONSTANT_VALUE)) {
                when (val descriptor = namesAndTypes[i].descriptor) {
                    INT_DESCRIPTOR -> constantPool.readInt(buf)
                    LONG_DESCRIPTOR -> constantPool.readLong(buf)
                    FLOAT_DESCRIPTOR -> constantPool.readFloat(buf)
                    DOUBLE_DESCRIPTOR -> constantPool.readDouble(buf)
                    STRING_DESCRIPTOR -> constantPool.readString(buf)
                    else -> throw IllegalArgumentException("Unsupported constant descriptor $descriptor")
                }
            } else {
                null
            }
        }
    }

    private fun readTryCatchStartPcs(
        buf: ByteBuf,
        attributes: Array<Array<String>>,
        counts: IntArray
    ): Array<IntArray?> {
        require(attributes.size == counts.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                var prevStartPc = 0

                IntArray(counts[i]) {
                    var startPc = buf.readUnsignedShort()

                    if (startPc == 0) {
                        startPc = prevStartPc
                    } else if (startPc == prevStartPc) {
                        startPc = 0
                    }

                    prevStartPc = startPc
                    startPc
                }
            } else {
                null
            }
        }
    }

    private fun readTryCatchEndPcsToHandlerPcs(
        buf: ByteBuf,
        attributes: Array<Array<String>>,
        counts: IntArray
    ): Array<IntArray?> {
        require(attributes.size == counts.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                IntArray(counts[i]) {
                    buf.readShort().toInt()
                }
            } else {
                null
            }
        }
    }

    private fun readTryCatchReverseHandlerPcs(
        buf: ByteBuf,
        attributes: Array<Array<String>>,
        counts: IntArray
    ): Array<IntArray?> {
        require(attributes.size == counts.size)

        return Array(attributes.size) { i ->
            if (attributes[i].contains(ConstantPool.CODE)) {
                IntArray(counts[i]) {
                    buf.readUnsignedShort()
                }
            } else {
                null
            }
        }
    }

    public fun write(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        require(clazz.version in SUPPORTED_VERSIONS) {
            val major = clazz.version and 0xFFFF
            val minor = (clazz.version shl 16) and 0xFFFF
            "Unsupported class version $major.$minor"
        }

        val insns = createInstructions(clazz)

        // write attributes
        writeFieldAttributeCounts(buf, clazz)
        writeMethodAttributeCounts(buf, clazz)

        writeFieldAttributes(buf, constantPool, clazz)
        writeMethodAttributes(buf, constantPool, clazz)

        // write method metadata
        writeExceptionCounts(buf, clazz)
        writeTryCatchCounts(buf, clazz)
        writeMaxStacks(buf, clazz)
        writeMaxLocals(buf, clazz)
        writeLinePcs(buf, clazz, insns)
        writeLineNumbers(buf, clazz)

        // write access flags
        buf.writeByte(clazz.access shr 8)

        writeFieldAccessFlagsMsb(buf, clazz)
        writeMethodAccessFlagsMsb(buf, clazz)

        buf.writeByte(clazz.access)

        writeFieldAccessFlagsLsb(buf, clazz)
        writeMethodAccessFlagsLsb(buf, clazz)

        // write source file
        constantPool.writeOptionalString(buf, clazz.sourceFile)

        // write exception types
        writeExceptions(buf, constantPool, clazz)
        writeTryCatchTypes(buf, constantPool, clazz)

        // write class and superclass name
        constantPool.writeString(buf, clazz.name)
        constantPool.writeString(buf, clazz.superName)

        // write interface names
        writeInterfaces(buf, constantPool, clazz)

        // write field and method names and types
        writeFieldNamesAndTypes(buf, constantPool, clazz)
        writeMethodNamesAndTypes(buf, constantPool, clazz)

        // write version
        buf.writeShort(clazz.version shr 16)
        buf.writeShort(clazz.version)

        // write constant field values
        writeConstants(buf, constantPool, clazz)

        // write try/catch blocks
        writeTryCatchStartPcs(buf, clazz, insns)
        writeTryCatchEndPcsToHandlerPcs(buf, clazz, insns)
        writeTryCatchReverseHandlerPcs(buf, clazz, insns)

        // write operand buffers
        Arena(buf.alloc()).use { alloc ->
            val newArrayBuf = alloc.buffer()
            val localVarBuf = alloc.buffer()
            val wideLocalVarBuf = alloc.buffer()
            val sipushAndSwitchBuf = alloc.buffer()
            val constantBuf = alloc.buffer()
            val wideConstantBuf = alloc.buffer()
            val classBuf = alloc.buffer()
            val fieldRefBuf = alloc.buffer()
            val methodRefBuf = alloc.buffer()
            val interfaceMethodRefBuf = alloc.buffer()
            val branchBuf = alloc.buffer()
            val bipushBuf = alloc.buffer()
            val wideIincBuf = alloc.buffer()
            val iincBuf = alloc.buffer()
            val multiNewArrayBuf = alloc.buffer()

            for ((i, list) in insns.withIndex()) {
                if (list.isEmpty()) {
                    continue
                }

                for ((pc, insn) in list.withIndex()) {
                    when (insn) {
                        is VarInsnNode -> {
                            when {
                                insn.`var` in 0..3 -> {
                                    when (insn.opcode) {
                                        Opcodes.ILOAD -> buf.writeByte(ILOAD_0 + insn.`var`)
                                        Opcodes.LLOAD -> buf.writeByte(LLOAD_0 + insn.`var`)
                                        Opcodes.FLOAD -> buf.writeByte(FLOAD_0 + insn.`var`)
                                        Opcodes.DLOAD -> buf.writeByte(DLOAD_0 + insn.`var`)
                                        Opcodes.ALOAD -> buf.writeByte(ALOAD_0 + insn.`var`)
                                        Opcodes.ISTORE -> buf.writeByte(ISTORE_0 + insn.`var`)
                                        Opcodes.LSTORE -> buf.writeByte(LSTORE_0 + insn.`var`)
                                        Opcodes.FSTORE -> buf.writeByte(FSTORE_0 + insn.`var`)
                                        Opcodes.DSTORE -> buf.writeByte(DSTORE_0 + insn.`var`)
                                        Opcodes.ASTORE -> buf.writeByte(ASTORE_0 + insn.`var`)
                                        else -> throw IllegalArgumentException(
                                            "Unsupported VarInsnNode opcode: ${insn.opcode}"
                                        )
                                    }
                                }

                                insn.`var` < 256 -> {
                                    buf.writeByte(insn.opcode)
                                    localVarBuf.writeByte(insn.`var`)
                                }

                                else -> {
                                    buf.writeByte(WIDE)
                                    buf.writeByte(insn.opcode)
                                    wideLocalVarBuf.writeShort(insn.`var`)
                                }
                            }
                        }

                        is LdcInsnNode -> {
                            when (val value = insn.cst) {
                                is Int -> {
                                    buf.writeByte(LDC_INT)
                                    constantPool.writeInt(constantBuf, value)
                                }

                                is Long -> {
                                    buf.writeByte(LDC_LONG)
                                    constantPool.writeLong(wideConstantBuf, value)
                                }

                                is Float -> {
                                    buf.writeByte(LDC_FLOAT)
                                    constantPool.writeFloat(constantBuf, value)
                                }

                                is Double -> {
                                    buf.writeByte(LDC_DOUBLE)
                                    constantPool.writeDouble(wideConstantBuf, value)
                                }

                                is String -> {
                                    buf.writeByte(LDC_STRING)
                                    constantPool.writeString(constantBuf, value)
                                }

                                is Type -> {
                                    if (value.sort == Type.OBJECT) {
                                        buf.writeByte(LDC_CLASS)
                                        constantPool.writeString(constantBuf, value.internalName)
                                    } else {
                                        throw IllegalArgumentException(
                                            "Unsupported constant type: ${value.sort}"
                                        )
                                    }
                                }

                                else -> throw IllegalArgumentException(
                                    "Unsupported constant type: ${value.javaClass.name}"
                                )
                            }
                        }

                        is TypeInsnNode -> {
                            buf.writeByte(insn.opcode)
                            constantPool.writeString(classBuf, insn.desc)
                        }

                        is FieldInsnNode -> {
                            buf.writeByte(insn.opcode)
                            constantPool.writeFieldRef(fieldRefBuf, MemberRef(insn.owner, insn.name, insn.desc))
                        }

                        is MethodInsnNode -> {
                            buf.writeByte(insn.opcode)

                            val methodRef = MemberRef(insn.owner, insn.name, insn.desc)
                            if (insn.itf) {
                                constantPool.writeInterfaceMethodRef(interfaceMethodRefBuf, methodRef)
                            } else {
                                constantPool.writeMethodRef(methodRefBuf, methodRef)
                            }
                        }

                        is JumpInsnNode -> {
                            val targetPc = insns[i].indexOf(insn.label.nextReal)
                            val delta = targetPc - pc

                            if (delta >= -32768 && delta <= 32767) {
                                buf.writeByte(insn.opcode)
                                branchBuf.writeShort(delta)
                            } else {
                                val wideOpcode = when (insn.opcode) {
                                    Opcodes.GOTO -> GOTO_W
                                    Opcodes.JSR -> JSR_W
                                    else -> throw IllegalArgumentException("Jump too far for opcode: ${insn.opcode}")
                                }

                                buf.writeByte(wideOpcode)
                                branchBuf.writeInt(delta)
                            }
                        }

                        is IntInsnNode -> {
                            buf.writeByte(insn.opcode)

                            when (insn.opcode) {
                                Opcodes.BIPUSH -> bipushBuf.writeByte(insn.operand)
                                Opcodes.SIPUSH -> sipushAndSwitchBuf.writeShort(insn.operand)
                                Opcodes.NEWARRAY -> newArrayBuf.writeByte(insn.operand)
                                else -> throw IllegalArgumentException("Unsupported IntInsnNode opcode: ${insn.opcode}")
                            }
                        }

                        is IincInsnNode -> {
                            if (insn.`var` < 256 && (insn.incr >= -128 && insn.incr <= 127)) {
                                buf.writeByte(insn.opcode)
                                localVarBuf.writeByte(insn.`var`)
                                iincBuf.writeByte(insn.incr)
                            } else {
                                buf.writeByte(WIDE)
                                buf.writeByte(insn.opcode)
                                wideLocalVarBuf.writeShort(insn.`var`)
                                wideIincBuf.writeShort(insn.incr)
                            }
                        }

                        is TableSwitchInsnNode -> {
                            buf.writeByte(insn.opcode)

                            val defaultPc = insns[i].indexOf(insn.dflt.nextReal)
                            branchBuf.writeInt(defaultPc - pc)

                            sipushAndSwitchBuf.writeInt(insn.min)
                            buf.writeVarInt(insn.max - insn.min)

                            for (target in insn.labels) {
                                val targetPc = insns[i].indexOf(target.nextReal)
                                branchBuf.writeInt(targetPc - pc)
                            }
                        }

                        is LookupSwitchInsnNode -> {
                            buf.writeByte(insn.opcode)

                            val defaultPc = insns[i].indexOf(insn.dflt.nextReal)
                            branchBuf.writeInt(defaultPc - pc)

                            val cases = insn.keys.size
                            buf.writeVarInt(cases)

                            var prevKey = 0
                            for (key in insn.keys) {
                                sipushAndSwitchBuf.writeInt(key - prevKey)
                                prevKey = key
                            }

                            for (target in insn.labels) {
                                val targetPc = insns[i].indexOf(target.nextReal)
                                branchBuf.writeInt(targetPc - pc)
                            }
                        }

                        is MultiANewArrayInsnNode -> {
                            buf.writeByte(insn.opcode)
                            constantPool.writeString(classBuf, insn.desc)
                            multiNewArrayBuf.writeByte(insn.dims)
                        }

                        is InsnNode -> buf.writeByte(insn.opcode)
                        else -> throw IllegalArgumentException("Unsupported instruction type: ${insn.javaClass.name}")
                    }
                }

                buf.writeByte(EOF)
            }

            buf.writeBytes(newArrayBuf)
            buf.writeBytes(localVarBuf)
            buf.writeBytes(wideLocalVarBuf)
            buf.writeBytes(sipushAndSwitchBuf)
            buf.writeBytes(constantBuf)
            buf.writeBytes(wideConstantBuf)
            buf.writeBytes(classBuf)
            buf.writeBytes(fieldRefBuf)
            buf.writeBytes(methodRefBuf)
            buf.writeBytes(interfaceMethodRefBuf)
            buf.writeBytes(branchBuf)
            buf.writeBytes(bipushBuf)
            buf.writeBytes(wideIincBuf)
            buf.writeBytes(iincBuf)
            buf.writeBytes(multiNewArrayBuf)
        }

        // write trailer
        buf.writeShort(clazz.interfaces.size)
        buf.writeShort(clazz.fields.size)
        buf.writeShort(clazz.methods.size)
    }

    private fun createInstructions(clazz: ClassNode): Array<Array<AbstractInsnNode>> {
        return Array(clazz.methods.size) { i ->
            clazz.methods[i].instructions.filter { it.opcode != -1 }.toTypedArray()
        }
    }

    private fun writeFieldAttributeCounts(buf: ByteBuf, clazz: ClassNode) {
        for (field in clazz.fields) {
            var attributes = 0

            if ((field.access and Opcodes.ACC_SYNTHETIC) != 0) {
                attributes++
            }

            if (field.value != null) {
                attributes++
            }

            buf.writeByte(attributes)
        }
    }

    private fun writeMethodAttributeCounts(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            var attributes = 0

            if ((method.access and Opcodes.ACC_SYNTHETIC) != 0) {
                attributes++
            }

            if (method.instructions.size() != 0) {
                attributes++
            }

            if (method.exceptions.isNotEmpty()) {
                attributes++
            }

            buf.writeByte(attributes)
        }
    }

    private fun writeFieldAttributes(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (field in clazz.fields) {
            if ((field.access and Opcodes.ACC_SYNTHETIC) != 0) {
                constantPool.writeString(buf, ConstantPool.SYNTHETIC)
            }

            if (field.value != null) {
                constantPool.writeString(buf, ConstantPool.CONSTANT_VALUE)
            }
        }
    }

    private fun writeMethodAttributes(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (method in clazz.methods) {
            if ((method.access and Opcodes.ACC_SYNTHETIC) != 0) {
                constantPool.writeString(buf, ConstantPool.SYNTHETIC)
            }

            if (method.instructions.size() != 0) {
                constantPool.writeString(buf, ConstantPool.CODE)
            }

            if (method.exceptions.isNotEmpty()) {
                constantPool.writeString(buf, ConstantPool.EXCEPTIONS)
            }
        }
    }

    private fun writeExceptionCounts(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            if (method.exceptions.isNotEmpty()) {
                buf.writeByte(method.exceptions.size)
            }
        }
    }

    private fun writeTryCatchCounts(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            if (method.instructions.size() != 0) {
                buf.writeUnsignedShortSmart(method.tryCatchBlocks.size)
            }
        }
    }

    private fun writeMaxStacks(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            if (method.instructions.size() != 0) {
                buf.writeByte(method.maxStack)
            }
        }
    }

    private fun writeMaxLocals(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            if (method.instructions.size() != 0) {
                buf.writeByte(method.maxLocals)
            }
        }
    }

    private fun writeLinePcs(buf: ByteBuf, clazz: ClassNode, insns: Array<Array<AbstractInsnNode>>) {
        for ((i, method) in clazz.methods.withIndex()) {
            if (method.instructions.size() == 0) {
                continue
            }

            var prevPc = -1

            for (insn in method.instructions) {
                if (insn !is LineNumberNode) {
                    continue
                }

                val pc = insns[i].indexOf(insn.nextReal)
                buf.writeUnsignedShortSmart(pc - prevPc)
                prevPc = pc
            }

            buf.writeUnsignedShortSmart(0)
        }
    }

    private fun writeLineNumbers(buf: ByteBuf, clazz: ClassNode) {
        var prevLine = 0

        for (method in clazz.methods) {
            if (method.instructions.size() == 0) {
                continue
            }

            for (insn in method.instructions) {
                if (insn !is LineNumberNode) {
                    continue
                }

                val line = insn.line
                buf.writeShort(line - prevLine)
                prevLine = line
            }
        }
    }

    private fun writeFieldAccessFlagsMsb(buf: ByteBuf, clazz: ClassNode) {
        for (field in clazz.fields) {
            buf.writeByte(field.access shr 8)
        }
    }

    private fun writeMethodAccessFlagsMsb(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            buf.writeByte(method.access shr 8)
        }
    }

    private fun writeFieldAccessFlagsLsb(buf: ByteBuf, clazz: ClassNode) {
        for (field in clazz.fields) {
            buf.writeByte(field.access)
        }
    }

    private fun writeMethodAccessFlagsLsb(buf: ByteBuf, clazz: ClassNode) {
        for (method in clazz.methods) {
            buf.writeByte(method.access)
        }
    }

    private fun writeExceptions(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (method in clazz.methods) {
            for (exception in method.exceptions) {
                constantPool.writeString(buf, exception)
            }
        }
    }

    private fun writeTryCatchTypes(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (method in clazz.methods) {
            for (tryCatch in method.tryCatchBlocks) {
                constantPool.writeOptionalString(buf, tryCatch.type)
            }
        }
    }

    private fun writeInterfaces(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (name in clazz.interfaces) {
            constantPool.writeString(buf, name)
        }
    }

    private fun writeFieldNamesAndTypes(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (field in clazz.fields) {
            constantPool.writeFieldNameAndType(buf, NameAndType(field.name, field.desc))
        }
    }

    private fun writeMethodNamesAndTypes(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (method in clazz.methods) {
            constantPool.writeMethodNameAndType(buf, NameAndType(method.name, method.desc))
        }
    }

    private fun writeConstants(buf: ByteBuf, constantPool: ConstantPool, clazz: ClassNode) {
        for (field in clazz.fields) {
            val value = field.value ?: continue

            when (value) {
                is Int -> constantPool.writeInt(buf, value)
                is Long -> constantPool.writeLong(buf, value)
                is Float -> constantPool.writeFloat(buf, value)
                is Double -> constantPool.writeDouble(buf, value)
                is String -> constantPool.writeString(buf, value)
                is Type -> {
                    if (value.sort == Type.OBJECT) {
                        constantPool.writeString(buf, value.internalName)
                    } else {
                        throw IllegalArgumentException("Unsupported constant type: ${value.sort}")
                    }
                }

                else -> throw IllegalArgumentException("Unsupported constant type: ${value.javaClass.name}")
            }
        }
    }

    private fun writeTryCatchStartPcs(buf: ByteBuf, clazz: ClassNode, insns: Array<Array<AbstractInsnNode>>) {
        for ((i, method) in clazz.methods.withIndex()) {
            var prevStartPc = 0

            for (tryCatch in method.tryCatchBlocks) {
                val startPc = insns[i].indexOf(tryCatch.start.nextReal)

                if (startPc == 0) {
                    buf.writeShort(prevStartPc)
                } else if (startPc == prevStartPc) {
                    buf.writeShort(0)
                } else {
                    buf.writeShort(startPc)
                }

                prevStartPc = startPc
            }
        }
    }

    private fun writeTryCatchEndPcsToHandlerPcs(buf: ByteBuf, clazz: ClassNode, insns: Array<Array<AbstractInsnNode>>) {
        for ((i, method) in clazz.methods.withIndex()) {
            for (tryCatch in method.tryCatchBlocks) {
                val list = insns[i]

                val endPc = list.indexOf(tryCatch.end.nextReal)
                val handlerPc = list.indexOf(tryCatch.handler.nextReal)

                buf.writeShort(handlerPc - endPc)
            }
        }
    }

    private fun writeTryCatchReverseHandlerPcs(buf: ByteBuf, clazz: ClassNode, insns: Array<Array<AbstractInsnNode>>) {
        for ((i, method) in clazz.methods.withIndex()) {
            for (tryCatch in method.tryCatchBlocks) {
                val list = insns[i]
                val handlerPc = list.indexOf(tryCatch.handler.nextReal)
                buf.writeShort(list.size - handlerPc)
            }
        }
    }
}
