package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.checkedAsInt
import dev.openrs2.deob.ast.util.checkedAsLong
import dev.openrs2.deob.ast.util.toLongLiteralExpr
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class BitMaskTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val shiftOp = expr.operator
            val bitwiseExpr = expr.left
            val shamtExpr = expr.right

            if (shiftOp !in SHIFT_OPS || bitwiseExpr !is BinaryExpr || shamtExpr !is IntegerLiteralExpr) {
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
    }

    private companion object {
        private val SHIFT_OPS = setOf(
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
