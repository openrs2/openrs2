package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import dev.openrs2.deob.ast.util.walk

class EncloseTransformer : Transformer() {
    private enum class Associativity {
        LEFT,
        RIGHT,
        NONE
    }

    private enum class Op(val associativity: Associativity) {
        ACCESS_PARENS(Associativity.LEFT),
        POSTFIX(Associativity.NONE),
        UNARY(Associativity.RIGHT),
        CAST_NEW(Associativity.RIGHT),
        MULTIPLICATIVE(Associativity.LEFT),
        ADDITIVE(Associativity.LEFT),
        SHIFT(Associativity.LEFT),
        RELATIONAL(Associativity.LEFT),
        EQUALITY(Associativity.NONE),
        BITWISE_AND(Associativity.LEFT),
        BITWISE_XOR(Associativity.LEFT),
        BITWISE_OR(Associativity.LEFT),
        LOGICAL_AND(Associativity.LEFT),
        LOGICAL_OR(Associativity.LEFT),
        TERNARY(Associativity.RIGHT),
        ASSIGNMENT(Associativity.RIGHT);

        fun isPrecedenceLess(other: Op): Boolean {
            return ordinal > other.ordinal
        }

        fun isPrecedenceLessEqual(other: Op): Boolean {
            return ordinal >= other.ordinal
        }

        companion object {
            fun from(expr: Expression): Op? {
                return when {
                    expr.isArrayAccessExpr || expr.isFieldAccessExpr -> ACCESS_PARENS
                    expr.isMethodCallExpr || expr.isEnclosedExpr -> ACCESS_PARENS
                    expr.isUnaryExpr -> if (expr.asUnaryExpr().operator.isPostfix) POSTFIX else UNARY
                    expr.isCastExpr || expr.isObjectCreationExpr || expr.isArrayCreationExpr -> CAST_NEW
                    expr.isBinaryExpr -> when (expr.asBinaryExpr().operator) {
                        BinaryExpr.Operator.MULTIPLY -> MULTIPLICATIVE
                        BinaryExpr.Operator.DIVIDE, BinaryExpr.Operator.REMAINDER -> MULTIPLICATIVE
                        BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MINUS -> ADDITIVE
                        BinaryExpr.Operator.LEFT_SHIFT -> SHIFT
                        BinaryExpr.Operator.SIGNED_RIGHT_SHIFT, BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT -> SHIFT
                        BinaryExpr.Operator.LESS, BinaryExpr.Operator.LESS_EQUALS -> RELATIONAL
                        BinaryExpr.Operator.GREATER, BinaryExpr.Operator.GREATER_EQUALS -> RELATIONAL
                        BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> EQUALITY
                        BinaryExpr.Operator.BINARY_AND -> BITWISE_AND
                        BinaryExpr.Operator.XOR -> BITWISE_XOR
                        BinaryExpr.Operator.BINARY_OR -> BITWISE_OR
                        BinaryExpr.Operator.AND -> LOGICAL_AND
                        BinaryExpr.Operator.OR -> LOGICAL_OR
                        else -> null
                    }
                    expr.isInstanceOfExpr -> RELATIONAL
                    expr.isConditionalExpr -> TERNARY
                    expr.isAssignExpr -> ASSIGNMENT
                    else -> null
                }
            }
        }
    }

    override fun transform(unit: CompilationUnit) {
        unit.walk { expr: Expression ->
            when {
                expr.isArrayAccessExpr -> {
                    val accessExpr = expr.asArrayAccessExpr()
                    encloseLeft(expr, accessExpr.name)
                }
                expr.isFieldAccessExpr -> {
                    encloseLeft(expr, expr.asFieldAccessExpr().scope)
                }
                expr.isMethodCallExpr -> {
                    expr.asMethodCallExpr().scope.ifPresent { scope ->
                        encloseLeft(expr, scope)
                    }
                }
                expr.isUnaryExpr -> {
                    val unaryExpr = expr.asUnaryExpr()
                    encloseRight(expr, unaryExpr.expression)
                }
                expr.isCastExpr -> {
                    encloseRight(expr, expr.asCastExpr().expression)
                }
                expr.isObjectCreationExpr -> {
                    expr.asObjectCreationExpr().scope.ifPresent { scope ->
                        encloseLeft(expr, scope)
                    }
                }
                expr.isBinaryExpr -> {
                    val binaryExpr = expr.asBinaryExpr()
                    encloseLeft(expr, binaryExpr.left)
                    encloseRight(expr, binaryExpr.right)
                }
                expr.isInstanceOfExpr -> {
                    encloseLeft(expr, expr.asInstanceOfExpr().expression)
                }
                expr.isConditionalExpr -> {
                    val conditionalExpr = expr.asConditionalExpr()
                    encloseLeft(expr, conditionalExpr.condition)
                    encloseLeft(expr, conditionalExpr.thenExpr)
                    encloseRight(expr, conditionalExpr.elseExpr)
                }
                expr.isAssignExpr -> {
                    val assignExpr = expr.asAssignExpr()
                    encloseLeft(expr, assignExpr.target)
                    encloseRight(expr, assignExpr.value)
                }
            }
        }
    }

    companion object {
        private fun encloseLeft(parent: Expression, child: Expression) {
            val parentOp = Op.from(parent) ?: throw IllegalArgumentException()
            val childOp = Op.from(child) ?: return

            when (parentOp.associativity) {
                Associativity.LEFT -> {
                    if (childOp.isPrecedenceLess(parentOp)) {
                        parent.replace(child, EnclosedExpr(child.clone()))
                    }
                }
                Associativity.NONE, Associativity.RIGHT -> {
                    if (childOp.isPrecedenceLessEqual(parentOp)) {
                        parent.replace(child, EnclosedExpr(child.clone()))
                    }
                }
            }
        }

        private fun encloseRight(parent: Expression, child: Expression) {
            val parentOp = Op.from(parent) ?: throw IllegalArgumentException()
            val childOp = Op.from(child) ?: return

            when (parentOp.associativity) {
                Associativity.NONE, Associativity.LEFT -> {
                    if (childOp.isPrecedenceLessEqual(parentOp)) {
                        parent.replace(child, EnclosedExpr(child.clone()))
                    }
                }
                Associativity.RIGHT -> {
                    if (childOp.isPrecedenceLess(parentOp)) {
                        parent.replace(child, EnclosedExpr(child.clone()))
                    }
                }
            }
        }
    }
}
