package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import jakarta.inject.Singleton
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.checkedAsInt
import org.openrs2.deob.ast.util.checkedAsLong
import org.openrs2.deob.ast.util.toLongLiteralExpr
import org.openrs2.deob.ast.util.walk

@Singleton
public class BitMaskTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        /*
         * Transform:
         *
         *     (x & y) >> z
         *
         * to:
         *
         *     (x >> z) & (y >> z)
         *
         * For example:
         *
         *     (x & 0xFF00) >> 8
         *
         * to:
         *
         *     (x >> 8) & 0xFF
         */
        unit.walk { expr: BinaryExpr ->
            val shiftOp = expr.operator
            val bitwiseExpr = expr.left
            val shamtExpr = expr.right

            if (shiftOp !in RIGHT_SHIFT_OPS || bitwiseExpr !is BinaryExpr || shamtExpr !is IntegerLiteralExpr) {
                return@walk
            }

            val bitwiseOp = bitwiseExpr.operator
            val argExpr = bitwiseExpr.left
            var maskExpr = bitwiseExpr.right

            if (bitwiseOp !in BITWISE_OPS) {
                return@walk
            }

            val shamt = shamtExpr.checkedAsInt()
            when (maskExpr) {
                is IntegerLiteralExpr -> {
                    var mask = maskExpr.checkedAsInt()

                    mask = when (shiftOp) {
                        BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> mask shr shamt
                        BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> mask ushr shamt
                        else -> error("Invalid shiftOp")
                    }

                    maskExpr = IntegerLiteralExpr(mask.toString())
                }

                is LongLiteralExpr -> {
                    var mask = maskExpr.checkedAsLong()

                    mask = when (shiftOp) {
                        BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> mask shr shamt
                        BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> mask ushr shamt
                        else -> error("Invalid shiftOp")
                    }

                    maskExpr = mask.toLongLiteralExpr()
                }

                else -> return@walk
            }

            expr.replace(BinaryExpr(BinaryExpr(argExpr.clone(), shamtExpr.clone(), shiftOp), maskExpr, bitwiseOp))
        }

        /*
         * Transform:
         *
         *     (x << y) & z
         *
         * to:
         *
         *     (x & (z >>> y)) << y
         *
         * For example:
         *
         *     (x << 8) & 0xFF00
         *
         * to:
         *
         *     (x & 0xFF) << 8
         */
        unit.walk { expr: BinaryExpr ->
            val bitwiseOp = expr.operator
            val shiftExpr = expr.left
            val maskExpr = expr.right

            if (bitwiseOp !in BITWISE_OPS || shiftExpr !is BinaryExpr) {
                return@walk
            }

            val shiftOp = shiftExpr.operator
            val argExpr = shiftExpr.left
            val shamtExpr = shiftExpr.right

            if (shiftOp != BinaryExpr.Operator.LEFT_SHIFT || shamtExpr !is IntegerLiteralExpr) {
                return@walk
            }

            val shamt = shamtExpr.checkedAsInt()
            val newMaskExpr = when (maskExpr) {
                is IntegerLiteralExpr -> {
                    var mask = maskExpr.checkedAsInt()
                    if (shamt > Integer.numberOfTrailingZeros(mask)) {
                        return@walk
                    }

                    mask = mask ushr shamt
                    IntegerLiteralExpr(mask.toString())
                }

                is LongLiteralExpr -> {
                    var mask = maskExpr.checkedAsLong()
                    if (shamt > java.lang.Long.numberOfTrailingZeros(mask)) {
                        return@walk
                    }

                    mask = mask ushr shamt
                    mask.toLongLiteralExpr()
                }

                else -> return@walk
            }

            expr.replace(BinaryExpr(BinaryExpr(argExpr.clone(), newMaskExpr, bitwiseOp), shamtExpr.clone(), shiftOp))
        }
    }

    private companion object {
        private val RIGHT_SHIFT_OPS = setOf(
            BinaryExpr.Operator.SIGNED_RIGHT_SHIFT,
            BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT
        )
        private val BITWISE_OPS = setOf(
            BinaryExpr.Operator.BINARY_AND,
            BinaryExpr.Operator.BINARY_OR,
            BinaryExpr.Operator.XOR
        )
    }
}
