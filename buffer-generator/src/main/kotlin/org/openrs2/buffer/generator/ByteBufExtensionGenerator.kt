package org.openrs2.buffer.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

public class ByteBufExtensionGenerator {
    public fun generate(): String {
        val builder = FileSpec.builder("org.openrs2.buffer", "GeneratedByteBufExtensions")
        builder.indent("    ")
        builder.addFileComment("This file is generated automatically. DO NOT EDIT.")

        for (type in IntType.entries) {
            for (order in ByteOrder.entries) {
                for (transformation in Transformation.entries) {
                    // only integers can be middle-endian
                    if (type != IntType.INT && (order == ByteOrder.ALT3 || order == ByteOrder.ALT3_REVERSE)) {
                        continue
                    }

                    // only bytes and shorts can be transformed
                    if (type != IntType.BYTE && type != IntType.SHORT && transformation != Transformation.IDENTITY) {
                        continue
                    }

                    // supplied by Netty
                    if (
                        (order == ByteOrder.LITTLE || order == ByteOrder.BIG) &&
                        transformation == Transformation.IDENTITY
                    ) {
                        continue
                    }

                    // byte order doesn't make sense for individual bytes
                    if (type == IntType.BYTE && order != ByteOrder.BIG) {
                        continue
                    }

                    for (signedness in Signedness.entries) {
                        // unsigned integers not supported
                        if (type == IntType.INT && signedness == Signedness.UNSIGNED) {
                            continue
                        }

                        builder.addFunction(createGetIntFunction(type, order, transformation, signedness))
                        builder.addFunction(createReadIntFunction(type, order, transformation, signedness))
                    }

                    builder.addFunction(createSetIntFunction(type, order, transformation))
                    builder.addFunction(createWriteIntFunction(type, order, transformation))
                }
            }
        }

        for (order in ArrayOrder.entries) {
            for (transformation in Transformation.entries) {
                // supplied by Netty
                if (order == ArrayOrder.FORWARD && transformation == Transformation.IDENTITY) {
                    continue
                }

                builder.addFunction(createGetArrayFunction(order, transformation))
                builder.addFunction(createGetArrayIndexLenFunction(order, transformation))
                builder.addFunction(createGetByteBufFunction(order, transformation))
                builder.addFunction(createGetByteBufLenFunction(order, transformation))
                builder.addFunction(createGetByteBufIndexLenFunction(order, transformation))

                builder.addFunction(createReadArrayFunction(order, transformation))
                builder.addFunction(createReadArrayIndexLenFunction(order, transformation))
                builder.addFunction(createReadByteBufFunction(order, transformation))
                builder.addFunction(createReadByteBufLenFunction(order, transformation))
                builder.addFunction(createReadByteBufIndexLenFunction(order, transformation))
                builder.addFunction(createReadByteBufAllocFunction(order, transformation))

                builder.addFunction(createSetArrayFunction(order, transformation))
                builder.addFunction(createSetArrayIndexLenFunction(order, transformation))
                builder.addFunction(createSetByteBufFunction(order, transformation))
                builder.addFunction(createSetByteBufLenFunction(order, transformation))
                builder.addFunction(createSetByteBufIndexLenFunction(order, transformation))

                builder.addFunction(createWriteArrayFunction(order, transformation))
                builder.addFunction(createWriteArrayIndexLenFunction(order, transformation))
                builder.addFunction(createWriteByteBufFunction(order, transformation))
                builder.addFunction(createWriteByteBufLenFunction(order, transformation))
                builder.addFunction(createWriteByteBufIndexLenFunction(order, transformation))
            }
        }

        for (transformation in Transformation.entries) {
            // supplied by Netty
            if (transformation == Transformation.IDENTITY) {
                continue
            }

            builder.addFunction(createGetBooleanFunction(transformation))
            builder.addFunction(createReadBooleanFunction(transformation))
            builder.addFunction(createSetBooleanFunction(transformation))
            builder.addFunction(createWriteBooleanFunction(transformation))
        }

        val file = builder.build()
        return file.toString()
            .replace("import kotlin.Boolean\n", "")
            .replace("import kotlin.Byte\n", "")
            .replace("import kotlin.ByteArray\n", "")
            .replace("import kotlin.Int\n", "")
            .replace("import kotlin.Short\n", "")
            .replace("import kotlin.Unit\n", "")
            .replace(": Unit {", " {")
    }

    private fun createGetIntFunction(
        type: IntType,
        order: ByteOrder,
        transformation: Transformation,
        signedness: Signedness
    ): FunSpec {
        val name = "${signedness.prefix}${type.prettyName}${order.suffix}${transformation.suffix}"

        val builder = FunSpec.builder("get$name")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)

        val readType = type.getReadType(signedness)
        builder.returns(readType)

        builder.addStatement("var value = 0")

        for (i in 0 until type.width) {
            val shift = order.getShift(i, type.width)

            val pre: String
            val post: String
            if (shift == 0) {
                pre = transformation.preTransform
                post = transformation.postReadTransform
            } else {
                pre = ""
                post = ""
            }

            builder.addStatement(
                "value = value or (((${pre}getByte(index + %L).toInt()$post) and 0xFF) shl %L)",
                i,
                shift
            )
        }

        builder.addStatement("return value.%N()", "to${readType.simpleName}")

        return builder.build()
    }

    private fun createReadIntFunction(
        type: IntType,
        order: ByteOrder,
        transformation: Transformation,
        signedness: Signedness
    ): FunSpec {
        val name = "${signedness.prefix}${type.prettyName}${order.suffix}${transformation.suffix}"

        val builder = FunSpec.builder("read$name")
        builder.receiver(ByteBuf::class)
        builder.returns(type.getReadType(signedness))

        builder.addStatement("val index = readerIndex()")
        builder.addStatement("val result = %N(index)", "get$name")
        builder.addStatement("readerIndex(index + %L)", type.width)
        builder.addStatement("return result")

        return builder.build()
    }

    private fun createSetIntFunction(type: IntType, order: ByteOrder, transformation: Transformation): FunSpec {
        val name = "${type.prettyName}${order.suffix}${transformation.suffix}"

        val builder = FunSpec.builder("set$name")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("value", type.writeType)
        builder.returns(ByteBuf::class)

        for (i in 0 until type.width) {
            val shift = order.getShift(i, type.width)

            val pre: String
            val post: String
            if (shift == 0) {
                pre = transformation.preTransform
                post = transformation.postWriteTransform
            } else {
                pre = ""
                post = ""
            }

            builder.addStatement("setByte(index + %L, $pre(value shr %L)$post)", i, shift)
        }

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteIntFunction(type: IntType, order: ByteOrder, transformation: Transformation): FunSpec {
        val name = "${type.prettyName}${order.suffix}${transformation.suffix}"

        val builder = FunSpec.builder("write$name")
        builder.receiver(ByteBuf::class)
        builder.addParameter("value", type.writeType)
        builder.returns(ByteBuf::class)

        builder.addStatement("val index = writerIndex()")
        builder.addStatement("ensureWritable(%L)", type.width)
        builder.addStatement("%N(index, value)", "set$name")
        builder.addStatement("writerIndex(index + %L)", type.width)
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetBooleanFunction(transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBoolean$transformation")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.returns(Boolean::class)

        builder.addStatement("return %N(index).toInt() != 0", "getByte$transformation")

        return builder.build()
    }

    private fun createReadBooleanFunction(transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBoolean$transformation")
        builder.receiver(ByteBuf::class)
        builder.returns(Boolean::class)

        builder.addStatement("return %N().toInt() != 0", "readByte$transformation")

        return builder.build()
    }

    private fun createSetBooleanFunction(transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBoolean$transformation")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("value", Boolean::class)
        builder.returns(ByteBuf::class)

        builder.beginControlFlow("if (value)")
        builder.addStatement("%N(index, 1)", "setByte$transformation")
        builder.nextControlFlow("else")
        builder.addStatement("%N(index, 0)", "setByte$transformation")
        builder.endControlFlow()

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteBooleanFunction(transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBoolean$transformation")
        builder.receiver(ByteBuf::class)
        builder.addParameter("value", Boolean::class)
        builder.returns(ByteBuf::class)

        builder.beginControlFlow("if (value)")
        builder.addStatement("%N(1)", "writeByte$transformation")
        builder.nextControlFlow("else")
        builder.addStatement("%N(0)", "writeByte$transformation")
        builder.endControlFlow()

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetArrayFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("dst", ByteArray::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("%N(index, dst, 0, dst.size)", "getBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetArrayIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("dst", ByteArray::class)
        builder.addParameter("dstIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("getBytes(index, dst, dstIndex, len)")
        builder.beginControlFlow("for (i in 0 until len)")
        builder.addStatement(
            "dst[dstIndex + i] = " +
                "(${transformation.preTransform}dst[dstIndex + i].toInt()${transformation.postReadTransform}).toByte()"
        )
        builder.endControlFlow()

        if (order == ArrayOrder.REVERSE) {
            builder.addStatement("dst.reverse(dstIndex, dstIndex + len)")
        }

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetByteBufFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val dstIndex = dst.writerIndex()")
        builder.addStatement("val len = dst.writableBytes()")
        builder.addStatement("dst.ensureWritable(len)")
        builder.addStatement("%N(index, dst, dstIndex, len)", "getBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("dst.writerIndex(dstIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetByteBufLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val dstIndex = dst.writerIndex()")
        builder.addStatement("dst.ensureWritable(len)")
        builder.addStatement("%N(index, dst, dstIndex, len)", "getBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("dst.writerIndex(dstIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createGetByteBufIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("getBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.addParameter("dstIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        val srcIndex = if (order == ArrayOrder.FORWARD) {
            "i"
        } else {
            "len - i - 1"
        }

        builder.beginControlFlow("for (i in 0 until len)")
        builder.addStatement(
            "dst.setByte(dstIndex + i, %N(index + $srcIndex).toInt())",
            "getByte${transformation.suffix}"
        )
        builder.endControlFlow()

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createReadByteBufAllocFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.beginControlFlow("if (len == 0)")
        builder.addStatement("return %T.EMPTY_BUFFER", Unpooled::class)
        builder.endControlFlow()

        builder.beginControlFlow("alloc().buffer(len).use { dst ->")
        builder.addStatement("%N(dst, len)", "readBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("return dst.retain()")
        builder.endControlFlow()

        return builder.build()
    }

    private fun createReadArrayFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("dst", ByteArray::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("%N(dst, 0, dst.size)", "readBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createReadArrayIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("dst", ByteArray::class)
        builder.addParameter("dstIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val index = readerIndex()")
        builder.addStatement("%N(index, dst, dstIndex, len)", "getBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("readerIndex(index + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createReadByteBufFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val dstIndex = dst.writerIndex()")
        builder.addStatement("val len = dst.writableBytes()")
        builder.addStatement("dst.ensureWritable(len)")
        builder.addStatement("%N(dst, dstIndex, len)", "readBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("dst.writerIndex(dstIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createReadByteBufLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val dstIndex = dst.writerIndex()")
        builder.addStatement("dst.ensureWritable(len)")
        builder.addStatement("%N(dst, dstIndex, len)", "readBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("dst.writerIndex(dstIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createReadByteBufIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("readBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("dst", ByteBuf::class)
        builder.addParameter("dstIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val index = readerIndex()")
        builder.addStatement("%N(index, dst, dstIndex, len)", "getBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("readerIndex(index + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createSetArrayFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("src", ByteArray::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("%N(index, src, 0, src.size)", "setBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createSetArrayIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("src", ByteArray::class)
        builder.addParameter("srcIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.beginControlFlow("%T.wrappedBuffer(src).use { buf ->", Unpooled::class)
        builder.addStatement("%N(index, buf, srcIndex, len)", "setBytes${order.suffix}${transformation.suffix}")
        builder.endControlFlow()

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createSetByteBufFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("src", ByteBuf::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val srcIndex = src.readerIndex()")
        builder.addStatement("val len = src.readableBytes()")
        builder.addStatement("%N(index, src, srcIndex, len)", "setBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("src.readerIndex(srcIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createSetByteBufLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("src", ByteBuf::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val srcIndex = src.readerIndex()")
        builder.addStatement("%N(index, src, srcIndex, len)", "setBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("src.readerIndex(srcIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createSetByteBufIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("setBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("index", Int::class)
        builder.addParameter("src", ByteBuf::class)
        builder.addParameter("srcIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        val srcIndex = if (order == ArrayOrder.FORWARD) {
            "i"
        } else {
            "len - i - 1"
        }

        builder.beginControlFlow("for (i in 0 until len)")
        builder.addStatement(
            "%N(index + i, src.getByte(srcIndex + $srcIndex).toInt())",
            "setByte${transformation.suffix}"
        )
        builder.endControlFlow()

        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteArrayFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("src", ByteArray::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("%N(src, 0, src.size)", "writeBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteArrayIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("src", ByteArray::class)
        builder.addParameter("srcIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val index = writerIndex()")
        builder.addStatement("ensureWritable(len)")
        builder.addStatement("%N(index, src, srcIndex, len)", "setBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("writerIndex(index + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteByteBufFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("src", ByteBuf::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val srcIndex = src.readerIndex()")
        builder.addStatement("val len = src.readableBytes()")
        builder.addStatement("%N(src, srcIndex, len)", "writeBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("src.readerIndex(srcIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteByteBufLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("src", ByteBuf::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val srcIndex = src.readerIndex()")
        builder.addStatement("%N(src, srcIndex, len)", "writeBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("src.readerIndex(srcIndex + len)")
        builder.addStatement("return this")

        return builder.build()
    }

    private fun createWriteByteBufIndexLenFunction(order: ArrayOrder, transformation: Transformation): FunSpec {
        val builder = FunSpec.builder("writeBytes${order.suffix}${transformation.suffix}")
        builder.receiver(ByteBuf::class)
        builder.addParameter("src", ByteBuf::class)
        builder.addParameter("srcIndex", Int::class)
        builder.addParameter("len", Int::class)
        builder.returns(ByteBuf::class)

        builder.addStatement("val index = writerIndex()")
        builder.addStatement("ensureWritable(len)")
        builder.addStatement("%N(index, src, srcIndex, len)", "setBytes${order.suffix}${transformation.suffix}")
        builder.addStatement("writerIndex(index + len)")
        builder.addStatement("return this")

        return builder.build()
    }
}
