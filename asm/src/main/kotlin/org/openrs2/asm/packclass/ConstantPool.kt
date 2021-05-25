package org.openrs2.asm.packclass

import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.doubles.DoubleAVLTreeSet
import it.unimi.dsi.fastutil.floats.FloatAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.openrs2.util.charset.ModifiedUtf8Charset

public class ConstantPool private constructor(
    private val strings: Array<String>,
    private val fieldNamesAndTypes: Array<NameAndType>,
    private val methodNamesAndTypes: Array<NameAndType>,
    private val fieldRefs: Array<MemberRef>,
    private val methodRefs: Array<MemberRef>,
    private val interfaceMethodRefs: Array<MemberRef>,
    private val ints: IntArray,
    private val longs: LongArray,
    private val floats: FloatArray,
    private val doubles: DoubleArray
) {
    init {
        require(strings.size <= 65534) // must leave a spare slot for readOptionalString
        require(fieldNamesAndTypes.size <= 65535)
        require(methodNamesAndTypes.size <= 65535)
        require(fieldRefs.size <= 65535)
        require(methodRefs.size <= 65535)
        require(interfaceMethodRefs.size <= 65535)
        require(ints.size <= 65535)
        require(longs.size <= 65535)
        require(floats.size <= 65535)
        require(doubles.size <= 65535)
    }

    public class Builder {
        /*
         * Sorting the entries provides two benefits: (1) the constant pool
         * will compress better, as related entries are more likely to fit in
         * the window, and (2) we can use binary search to speed up searching
         * for an entry's index.
         */
        private val strings = ObjectAVLTreeSet<String>()
        private val fieldNamesAndTypes = ObjectAVLTreeSet<NameAndType>()
        private val methodNamesAndTypes = ObjectAVLTreeSet<NameAndType>()
        private val fieldRefs = ObjectAVLTreeSet<MemberRef>()
        private val methodRefs = ObjectAVLTreeSet<MemberRef>()
        private val interfaceMethodRefs = ObjectAVLTreeSet<MemberRef>()
        private val ints = IntAVLTreeSet()
        private val longs = LongAVLTreeSet()
        private val floats = FloatAVLTreeSet()
        private val doubles = DoubleAVLTreeSet()

        public fun add(clazz: ClassNode): Builder {
            if (clazz.sourceFile != null) {
                strings += clazz.sourceFile
            }

            strings += clazz.name
            strings += clazz.superName

            strings.addAll(clazz.interfaces)

            for (method in clazz.methods) {
                addMethodNameAndType(NameAndType(method.name, method.desc))

                strings += method.exceptions

                for (tryCatch in method.tryCatchBlocks) {
                    if (tryCatch.type != null) {
                        strings += tryCatch.type
                    }
                }

                for (insn in method.instructions) {
                    when (insn) {
                        is LdcInsnNode -> addConstant(insn.cst)
                        is MultiANewArrayInsnNode -> strings += insn.desc
                        is MethodInsnNode -> {
                            val methodRef = MemberRef(insn.owner, insn.name, insn.desc)
                            if (insn.itf) {
                                addInterfaceMethodRef(methodRef)
                            } else {
                                addMethodRef(methodRef)
                            }
                        }
                        is FieldInsnNode -> addFieldRef(MemberRef(insn.owner, insn.name, insn.desc))
                        is TypeInsnNode -> strings += insn.desc
                    }
                }
            }

            for (field in clazz.fields) {
                addFieldNameAndType(NameAndType(field.name, field.desc))

                if (field.value != null) {
                    addConstant(field.value)
                }
            }

            return this
        }

        private fun addFieldNameAndType(nameAndType: NameAndType) {
            strings += nameAndType.name
            strings += nameAndType.descriptor
            fieldNamesAndTypes += nameAndType
        }

        private fun addMethodNameAndType(nameAndType: NameAndType) {
            strings += nameAndType.name
            strings += nameAndType.descriptor
            methodNamesAndTypes += nameAndType
        }

        private fun addFieldRef(fieldRef: MemberRef) {
            strings += fieldRef.clazz
            addFieldNameAndType(fieldRef.nameAndType)
            fieldRefs += fieldRef
        }

        private fun addMethodRef(methodRef: MemberRef) {
            strings += methodRef.clazz
            addMethodNameAndType(methodRef.nameAndType)
            methodRefs += methodRef
        }

        private fun addInterfaceMethodRef(methodRef: MemberRef) {
            strings += methodRef.clazz
            addMethodNameAndType(methodRef.nameAndType)
            interfaceMethodRefs += methodRef
        }

        private fun addConstant(value: Any) {
            when (value) {
                is Int -> ints += value
                is Long -> longs += value
                is Float -> floats += value
                is Double -> doubles += value
                is String -> strings += value
                is Type -> {
                    if (value.sort == Type.OBJECT) {
                        strings += value.internalName
                    } else {
                        throw IllegalArgumentException("Unsupported constant type: ${value.sort}")
                    }
                }
                else -> throw IllegalArgumentException("Unsupported constant type: ${value.javaClass.name}")
            }
        }

        public fun build(): ConstantPool {
            strings.remove(CODE)
            strings.remove(EXCEPTIONS)
            strings.remove(SYNTHETIC)
            strings.remove(CONSTANT_VALUE)
            strings.remove(SOURCE_FILE)
            strings.remove(LINE_NUMBER_TABLE)

            val it = strings.iterator()
            val stringArray = Array(BUILTIN_STRINGS + strings.size) { i ->
                when (i) {
                    CODE_INDEX -> CODE
                    EXCEPTIONS_INDEX -> EXCEPTIONS
                    SYNTHETIC_INDEX -> SYNTHETIC
                    CONSTANT_VALUE_INDEX -> CONSTANT_VALUE
                    SOURCE_FILE_INDEX -> SOURCE_FILE
                    LINE_NUMBER_TABLE_INDEX -> LINE_NUMBER_TABLE
                    else -> it.next()
                }
            }

            check(!it.hasNext())

            return ConstantPool(
                stringArray,
                fieldNamesAndTypes.toTypedArray(),
                methodNamesAndTypes.toTypedArray(),
                fieldRefs.toTypedArray(),
                methodRefs.toTypedArray(),
                interfaceMethodRefs.toTypedArray(),
                ints.toIntArray(),
                longs.toLongArray(),
                floats.toFloatArray(),
                doubles.toDoubleArray()
            )
        }
    }

    public fun readString(buf: ByteBuf): String {
        val index = buf.readUnsignedShort()
        return strings[index]
    }

    private fun getStringIndex(value: String): Int {
        return when (value) {
            CODE -> CODE_INDEX
            EXCEPTIONS -> EXCEPTIONS_INDEX
            SYNTHETIC -> SYNTHETIC_INDEX
            CONSTANT_VALUE -> CONSTANT_VALUE_INDEX
            SOURCE_FILE -> SOURCE_FILE_INDEX
            LINE_NUMBER_TABLE -> LINE_NUMBER_TABLE_INDEX
            else -> strings.binarySearch(value, BUILTIN_STRINGS)
        }
    }

    public fun writeString(buf: ByteBuf, value: String) {
        buf.writeShort(getStringIndex(value))
    }

    public fun readOptionalString(buf: ByteBuf): String? {
        val index = buf.readUnsignedShort()
        return if (index != 0) {
            strings[index - 1]
        } else {
            null
        }
    }

    public fun writeOptionalString(buf: ByteBuf, value: String?) {
        if (value != null) {
            buf.writeShort(getStringIndex(value) + 1)
        } else {
            buf.writeShort(0)
        }
    }

    public fun readFieldNameAndType(buf: ByteBuf): NameAndType {
        val index = buf.readUnsignedShort()
        return fieldNamesAndTypes[index]
    }

    public fun writeFieldNameAndType(buf: ByteBuf, value: NameAndType) {
        buf.writeShort(fieldNamesAndTypes.binarySearch(value))
    }

    public fun readMethodNameAndType(buf: ByteBuf): NameAndType {
        val index = buf.readUnsignedShort()
        return methodNamesAndTypes[index]
    }

    public fun writeMethodNameAndType(buf: ByteBuf, value: NameAndType) {
        buf.writeShort(methodNamesAndTypes.binarySearch(value))
    }

    public fun readFieldRef(buf: ByteBuf): MemberRef {
        val index = buf.readUnsignedShort()
        return fieldRefs[index]
    }

    public fun writeFieldRef(buf: ByteBuf, value: MemberRef) {
        buf.writeShort(fieldRefs.binarySearch(value))
    }

    public fun readMethodRef(buf: ByteBuf): MemberRef {
        val index = buf.readUnsignedShort()
        return methodRefs[index]
    }

    public fun writeMethodRef(buf: ByteBuf, value: MemberRef) {
        buf.writeShort(methodRefs.binarySearch(value))
    }

    public fun readInterfaceMethodRef(buf: ByteBuf): MemberRef {
        val index = buf.readUnsignedShort()
        return interfaceMethodRefs[index]
    }

    public fun writeInterfaceMethodRef(buf: ByteBuf, value: MemberRef) {
        buf.writeShort(interfaceMethodRefs.binarySearch(value))
    }

    public fun readInt(buf: ByteBuf): Int {
        val index = buf.readUnsignedShort()
        return ints[index]
    }

    public fun writeInt(buf: ByteBuf, value: Int) {
        buf.writeShort(ints.binarySearch(value))
    }

    public fun readLong(buf: ByteBuf): Long {
        val index = buf.readUnsignedShort()
        return longs[index]
    }

    public fun writeLong(buf: ByteBuf, value: Long) {
        buf.writeShort(longs.binarySearch(value))
    }

    public fun readFloat(buf: ByteBuf): Float {
        val index = buf.readUnsignedShort()
        return floats[index]
    }

    public fun writeFloat(buf: ByteBuf, value: Float) {
        buf.writeShort(floats.binarySearch(value))
    }

    public fun readDouble(buf: ByteBuf): Double {
        val index = buf.readUnsignedShort()
        return doubles[index]
    }

    public fun writeDouble(buf: ByteBuf, value: Double) {
        buf.writeShort(doubles.binarySearch(value))
    }

    public fun write(buf: ByteBuf) {
        writeStrings(buf)

        writeNameAndTypeNames(buf, fieldNamesAndTypes)
        writeNameAndTypeNames(buf, methodNamesAndTypes)
        writeNameAndTypeDescriptors(buf, fieldNamesAndTypes)
        writeNameAndTypeDescriptors(buf, methodNamesAndTypes)

        writeMemberRefClasses(buf, fieldRefs)
        writeMemberRefClasses(buf, methodRefs)
        writeMemberRefClasses(buf, interfaceMethodRefs)
        writeFieldRefNamesAndTypes(buf)
        writeMethodRefNamesAndTypes(buf, methodRefs)
        writeMethodRefNamesAndTypes(buf, interfaceMethodRefs)

        writeInts(buf, ints)
        writeLongs(buf, longs)
        writeFloats(buf)
        writeDoubles(buf)

        buf.writeShort(strings.size)
        buf.writeShort(ints.size)
        buf.writeShort(longs.size)
        buf.writeShort(floats.size)
        buf.writeShort(doubles.size)
        buf.writeShort(fieldNamesAndTypes.size)
        buf.writeShort(methodNamesAndTypes.size)
        buf.writeShort(fieldRefs.size)
        buf.writeShort(methodRefs.size)
        buf.writeShort(interfaceMethodRefs.size)
    }

    private fun writeStrings(buf: ByteBuf) {
        for (i in BUILTIN_STRINGS until strings.size) {
            buf.writeString(strings[i], ModifiedUtf8Charset)
        }
    }

    private fun writeNameAndTypeNames(buf: ByteBuf, namesAndTypes: Array<NameAndType>) {
        for (nameAndType in namesAndTypes) {
            writeString(buf, nameAndType.name)
        }
    }

    private fun writeNameAndTypeDescriptors(buf: ByteBuf, namesAndTypes: Array<NameAndType>) {
        for (nameAndType in namesAndTypes) {
            writeString(buf, nameAndType.descriptor)
        }
    }

    private fun writeMemberRefClasses(buf: ByteBuf, memberRefs: Array<MemberRef>) {
        for (memberRef in memberRefs) {
            writeString(buf, memberRef.clazz)
        }
    }

    private fun writeFieldRefNamesAndTypes(buf: ByteBuf) {
        for (fieldRef in fieldRefs) {
            writeFieldNameAndType(buf, fieldRef.nameAndType)
        }
    }

    private fun writeMethodRefNamesAndTypes(buf: ByteBuf, methodRefs: Array<MemberRef>) {
        for (methodRef in methodRefs) {
            writeMethodNameAndType(buf, methodRef.nameAndType)
        }
    }

    private fun writeInts(buf: ByteBuf, values: IntArray) {
        for (i in 24 downTo 0 step 8) {
            var previous = 0
            for (value in values) {
                buf.writeByte((value - previous) shr i)
                previous = value
            }
        }
    }

    private fun writeLongs(buf: ByteBuf, values: LongArray) {
        for (i in 56 downTo 0 step 8) {
            var previous = 0L
            for (value in values) {
                buf.writeByte(((value - previous) shr i).toInt())
                previous = value
            }
        }
    }

    private fun writeFloats(buf: ByteBuf) {
        writeInts(buf, IntArray(floats.size) { i ->
            floats[i].toRawBits()
        })
    }

    private fun writeDoubles(buf: ByteBuf) {
        writeLongs(buf, LongArray(doubles.size) { i ->
            doubles[i].toRawBits()
        })
    }

    public companion object {
        public const val CODE: String = "Code"
        public const val EXCEPTIONS: String = "Exceptions"
        public const val SYNTHETIC: String = "Synthetic"
        public const val CONSTANT_VALUE: String = "ConstantValue"
        public const val SOURCE_FILE: String = "SourceFile"
        public const val LINE_NUMBER_TABLE: String = "LineNumberTable"

        public const val CODE_INDEX: Int = 0
        public const val EXCEPTIONS_INDEX: Int = 1
        public const val SYNTHETIC_INDEX: Int = 2
        public const val CONSTANT_VALUE_INDEX: Int = 3
        public const val SOURCE_FILE_INDEX: Int = 4
        public const val LINE_NUMBER_TABLE_INDEX: Int = 5

        public const val BUILTIN_STRINGS: Int = 6

        private const val TRAILER_LEN = 20

        public fun read(buf: ByteBuf): ConstantPool {
            // read trailer
            buf.markReaderIndex()
            buf.readerIndex(buf.writerIndex() - TRAILER_LEN)

            val stringCount = buf.readUnsignedShort()
            val intCount = buf.readUnsignedShort()
            val longCount = buf.readUnsignedShort()
            val floatCount = buf.readUnsignedShort()
            val doubleCount = buf.readUnsignedShort()
            val fieldNameAndTypeCount = buf.readUnsignedShort()
            val methodNameAndTypeCount = buf.readUnsignedShort()
            val fieldRefCount = buf.readUnsignedShort()
            val methodRefCount = buf.readUnsignedShort()
            val interfaceMethodRefCount = buf.readUnsignedShort()

            buf.resetReaderIndex()

            // read UTF-8 entries
            val strings = readStrings(buf, stringCount)

            // read NameAndType entries
            val fieldNames = readStringPointers(buf, fieldNameAndTypeCount, strings)
            val methodNames = readStringPointers(buf, methodNameAndTypeCount, strings)
            val fieldDescriptors = readStringPointers(buf, fieldNameAndTypeCount, strings)
            val methodDescriptors = readStringPointers(buf, methodNameAndTypeCount, strings)

            val fieldNamesAndTypes = createNamesAndTypes(fieldNames, fieldDescriptors)
            val methodNamesAndTypes = createNamesAndTypes(methodNames, methodDescriptors)

            // read FieldRef, MethodRef and InterfaceMethodRef entries
            val fieldRefClasses = readStringPointers(buf, fieldRefCount, strings)
            val methodRefClasses = readStringPointers(buf, methodRefCount, strings)
            val interfaceRefMethodClasses = readStringPointers(buf, interfaceMethodRefCount, strings)

            val fieldRefNamesAndTypes = readNameAndTypePointers(buf, fieldRefCount, fieldNamesAndTypes)
            val methodRefNamesAndTypes = readNameAndTypePointers(buf, methodRefCount, methodNamesAndTypes)
            val interfaceMethodRefNamesAndTypes = readNameAndTypePointers(
                buf,
                interfaceMethodRefCount,
                methodNamesAndTypes
            )

            val fieldRefs = createMembers(fieldRefClasses, fieldRefNamesAndTypes)
            val methodRefs = createMembers(methodRefClasses, methodRefNamesAndTypes)
            val interfaceMethodRefs = createMembers(interfaceRefMethodClasses, interfaceMethodRefNamesAndTypes)

            // read numeric entries
            val ints = readInts(buf, intCount)
            val longs = readLongs(buf, longCount)
            val floats = readFloats(buf, floatCount)
            val doubles = readDoubles(buf, doubleCount)

            // skip trailer
            buf.skipBytes(TRAILER_LEN)

            return ConstantPool(
                strings,
                fieldNamesAndTypes,
                methodNamesAndTypes,
                fieldRefs,
                methodRefs,
                interfaceMethodRefs,
                ints,
                longs,
                floats,
                doubles
            )
        }

        private fun readStrings(buf: ByteBuf, size: Int): Array<String> {
            require(size >= BUILTIN_STRINGS)

            return Array(size) { i ->
                when (i) {
                    CODE_INDEX -> CODE
                    EXCEPTIONS_INDEX -> EXCEPTIONS
                    SYNTHETIC_INDEX -> SYNTHETIC
                    CONSTANT_VALUE_INDEX -> CONSTANT_VALUE
                    SOURCE_FILE_INDEX -> SOURCE_FILE
                    LINE_NUMBER_TABLE_INDEX -> LINE_NUMBER_TABLE
                    else -> buf.readString(ModifiedUtf8Charset)
                }
            }
        }

        private fun readStringPointers(buf: ByteBuf, size: Int, entries: Array<String>): Array<String> {
            return Array(size) {
                val index = buf.readUnsignedShort()
                entries[index]
            }
        }

        private fun createNamesAndTypes(names: Array<String>, descriptors: Array<String>): Array<NameAndType> {
            check(names.size == descriptors.size)

            return Array(names.size) { i ->
                NameAndType(names[i], descriptors[i])
            }
        }

        private fun readNameAndTypePointers(buf: ByteBuf, size: Int, entries: Array<NameAndType>): Array<NameAndType> {
            return Array(size) {
                val index = buf.readUnsignedShort()
                entries[index]
            }
        }

        private fun createMembers(classes: Array<String>, namesAndTypes: Array<NameAndType>): Array<MemberRef> {
            check(classes.size == namesAndTypes.size)

            return Array(classes.size) { i ->
                MemberRef(classes[i], namesAndTypes[i])
            }
        }

        private fun readInts(buf: ByteBuf, size: Int): IntArray {
            val entries = IntArray(size)

            for (i in 24 downTo 0 step 8) {
                var accumulator = 0
                for (j in entries.indices) {
                    accumulator += buf.readUnsignedByte().toInt() shl i
                    entries[j] += accumulator
                }
            }

            return entries
        }

        private fun readLongs(buf: ByteBuf, size: Int): LongArray {
            val entries = LongArray(size)

            for (i in 56 downTo 0 step 8) {
                var accumulator = 0L
                for (j in entries.indices) {
                    accumulator += buf.readUnsignedByte().toLong() shl i
                    entries[j] += accumulator
                }
            }

            return entries
        }

        private fun readFloats(buf: ByteBuf, size: Int): FloatArray {
            val entries = readInts(buf, size)
            return FloatArray(entries.size) { i ->
                Float.fromBits(entries[i])
            }
        }

        private fun readDoubles(buf: ByteBuf, size: Int): DoubleArray {
            val entries = readLongs(buf, size)
            return DoubleArray(entries.size) { i ->
                Double.fromBits(entries[i])
            }
        }
    }
}
