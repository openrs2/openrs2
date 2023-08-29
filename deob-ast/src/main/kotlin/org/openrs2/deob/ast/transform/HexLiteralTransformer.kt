package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import jakarta.inject.Singleton
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.checkedAsInt
import org.openrs2.deob.ast.util.checkedAsLong
import org.openrs2.deob.ast.util.toHexLiteralExpr
import org.openrs2.deob.ast.util.walk

@Singleton
public class HexLiteralTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            if (expr.operator in SHIFT_OPS) {
                convertToHex(expr.left)
            }

            if (expr.operator in BITWISE_OPS) {
                convertToHex(expr.left)
                convertToHex(expr.right)
            }
        }

        unit.walk { expr: AssignExpr ->
            if (expr.operator in ASSIGN_OPS) {
                convertToHex(expr.value)
            }
        }
    }

    private fun convertToHex(expr: Expression) {
        when (expr) {
            is IntegerLiteralExpr -> expr.replace(expr.checkedAsInt().toHexLiteralExpr())
            is LongLiteralExpr -> expr.replace(expr.checkedAsLong().toHexLiteralExpr())
        }
    }

    private companion object {
        private val SHIFT_OPS = setOf(
            BinaryExpr.Operator.LEFT_SHIFT,
            BinaryExpr.Operator.SIGNED_RIGHT_SHIFT,
            BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT
        )
        private val BITWISE_OPS = setOf(
            BinaryExpr.Operator.BINARY_AND,
            BinaryExpr.Operator.BINARY_OR,
            BinaryExpr.Operator.XOR
        )
        private val ASSIGN_OPS = setOf(
            AssignExpr.Operator.BINARY_AND,
            AssignExpr.Operator.BINARY_OR,
            AssignExpr.Operator.LEFT_SHIFT,
            AssignExpr.Operator.SIGNED_RIGHT_SHIFT,
            AssignExpr.Operator.UNSIGNED_RIGHT_SHIFT,
            AssignExpr.Operator.XOR
        )
    }
}
