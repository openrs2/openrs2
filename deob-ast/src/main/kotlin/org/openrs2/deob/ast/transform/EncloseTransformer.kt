package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.UnaryExpr
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class EncloseTransformer : Transformer() {
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
                return when (expr) {
                    is ArrayAccessExpr, is FieldAccessExpr -> ACCESS_PARENS
                    is MethodCallExpr, is EnclosedExpr -> ACCESS_PARENS
                    is UnaryExpr -> if (expr.operator.isPostfix) POSTFIX else UNARY
                    is CastExpr, is ObjectCreationExpr, is ArrayCreationExpr -> CAST_NEW
                    is BinaryExpr -> when (expr.operator) {
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

                    is InstanceOfExpr -> RELATIONAL
                    is ConditionalExpr -> TERNARY
                    is AssignExpr -> ASSIGNMENT
                    else -> null
                }
            }
        }
    }

    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: Expression ->
            when (expr) {
                is ArrayAccessExpr -> encloseLeft(expr, expr.name)
                is FieldAccessExpr -> encloseLeft(expr, expr.scope)
                is MethodCallExpr -> {
                    expr.scope.ifPresent { scope ->
                        encloseLeft(expr, scope)
                    }
                }

                is UnaryExpr -> encloseRight(expr, expr.expression)
                is CastExpr -> encloseRight(expr, expr.expression)
                is ObjectCreationExpr -> {
                    expr.scope.ifPresent { scope ->
                        encloseLeft(expr, scope)
                    }
                }

                is BinaryExpr -> {
                    encloseLeft(expr, expr.left)
                    encloseRight(expr, expr.right)
                }

                is InstanceOfExpr -> encloseLeft(expr, expr.expression)
                is ConditionalExpr -> {
                    encloseLeft(expr, expr.condition)
                    encloseLeft(expr, expr.thenExpr)
                    encloseRight(expr, expr.elseExpr)
                }

                is AssignExpr -> {
                    encloseLeft(expr, expr.target)
                    encloseRight(expr, expr.value)
                }
            }
        }
    }

    private companion object {
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
