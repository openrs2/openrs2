package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.NodeUtils

class BitMaskTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, BinaryExpr::class.java) { expr ->
            val shiftOp = expr.operator
            val left = expr.left
            val shamtExpr = expr.right

            if (!SHIFT_OPS.contains(shiftOp) || !left.isBinaryExpr || !shamtExpr.isIntegerLiteralExpr) {
                return@walk
            }

            val bitwiseExpr = left.asBinaryExpr()
            val bitwiseOp = bitwiseExpr.operator
            val argExpr = bitwiseExpr.left
            var maskExpr = bitwiseExpr.right

            if (!BITWISE_OPS.contains(bitwiseOp) || !ExprUtils.isIntegerOrLongLiteral(maskExpr)) {
                return@walk
            }

            val shamt = shamtExpr.asIntegerLiteralExpr().asInt()
            if (maskExpr.isIntegerLiteralExpr) {
                var mask = maskExpr.asIntegerLiteralExpr().asInt()

                mask = when (shiftOp) {
                    BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> mask shr shamt
                    BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> mask ushr shamt
                    else -> throw IllegalStateException()
                }

                maskExpr = IntegerLiteralExpr(mask)
            } else {
                var mask = maskExpr.asLongLiteralExpr().asLong()

                mask = when (shiftOp) {
                    BinaryExpr.Operator.SIGNED_RIGHT_SHIFT -> mask shr shamt
                    BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> mask ushr shamt
                    else -> throw IllegalStateException()
                }

                maskExpr = ExprUtils.createLong(mask)
            }

            expr.replace(BinaryExpr(BinaryExpr(argExpr.clone(), shamtExpr.clone(), shiftOp), maskExpr, bitwiseOp))
        }
    }

    companion object {
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
