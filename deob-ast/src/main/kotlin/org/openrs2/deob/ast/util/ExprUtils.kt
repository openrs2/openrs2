package org.openrs2.deob.ast.util

import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr

public fun IntegerLiteralExpr.checkedAsInt(): Int {
    val n = asNumber()
    if (n !is Int) {
        error("Invalid IntegerLiteralExpr type")
    }
    return n
}

public fun Int.toHexLiteralExpr(): IntegerLiteralExpr {
    return IntegerLiteralExpr("0x${Integer.toUnsignedString(this, 16).uppercase()}")
}

public fun LongLiteralExpr.checkedAsLong(): Long {
    val n = asNumber()
    if (n !is Long) {
        error("Invalid LongLiteralExpr type")
    }
    return n
}

public fun Long.toHexLiteralExpr(): LongLiteralExpr {
    return LongLiteralExpr("0x${java.lang.Long.toUnsignedString(this, 16).uppercase()}L")
}

public fun Expression.isIntegerOrLongLiteral(): Boolean {
    return this is IntegerLiteralExpr || this is LongLiteralExpr
}

public fun Long.toLongLiteralExpr(): LongLiteralExpr {
    return LongLiteralExpr(this.toString() + "L")
}

public fun Expression.negate(): Expression {
    return when (this) {
        is UnaryExpr -> when (operator) {
            UnaryExpr.Operator.PLUS -> UnaryExpr(expression.clone(), UnaryExpr.Operator.MINUS)
            UnaryExpr.Operator.MINUS -> expression.clone()
            else -> UnaryExpr(clone(), UnaryExpr.Operator.MINUS)
        }

        is IntegerLiteralExpr -> when (val n = asNumber()) {
            IntegerLiteralExpr.MAX_31_BIT_UNSIGNED_VALUE_AS_LONG -> IntegerLiteralExpr(Integer.MIN_VALUE.toString())
            is Int -> IntegerLiteralExpr((-n.toInt()).toString())
            else -> error("Invalid IntegerLiteralExpr type")
        }

        is LongLiteralExpr -> when (val n = asNumber()) {
            LongLiteralExpr.MAX_63_BIT_UNSIGNED_VALUE_AS_BIG_INTEGER -> Long.MIN_VALUE.toLongLiteralExpr()
            is Long -> (-n).toLongLiteralExpr()
            else -> error("Invalid LongLiteralExpr type")
        }

        else -> UnaryExpr(clone(), UnaryExpr.Operator.MINUS)
    }
}

public fun Expression.not(): Expression {
    when (this) {
        is UnaryExpr -> {
            if (operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                return expression.clone()
            }
        }

        is BinaryExpr -> {
            when (operator) {
                BinaryExpr.Operator.EQUALS ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.NOT_EQUALS)

                BinaryExpr.Operator.NOT_EQUALS ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.EQUALS)

                BinaryExpr.Operator.GREATER ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.LESS_EQUALS)

                BinaryExpr.Operator.GREATER_EQUALS ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.LESS)

                BinaryExpr.Operator.LESS ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.GREATER_EQUALS)

                BinaryExpr.Operator.LESS_EQUALS ->
                    return BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.GREATER)

                BinaryExpr.Operator.AND ->
                    return BinaryExpr(left.not(), right.not(), BinaryExpr.Operator.OR)

                BinaryExpr.Operator.OR ->
                    return BinaryExpr(left.not(), right.not(), BinaryExpr.Operator.AND)

                else -> Unit
            }
        }

        is BooleanLiteralExpr -> return BooleanLiteralExpr(!value)
    }

    return UnaryExpr(clone(), UnaryExpr.Operator.LOGICAL_COMPLEMENT)
}

public fun Expression.countNots(): Int {
    var count = 0

    if (this is UnaryExpr && operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
        count++
    } else if (this is BinaryExpr && operator == BinaryExpr.Operator.NOT_EQUALS) {
        count++
    }

    for (child in findAll(Expression::class.java)) {
        if (child !== this) {
            count += child.countNots()
        }
    }

    return count
}

public fun Expression.hasSideEffects(): Boolean {
    return when (this) {
        is LiteralExpr, is NameExpr, is ThisExpr -> false
        is UnaryExpr -> expression.hasSideEffects()
        is BinaryExpr -> left.hasSideEffects() || right.hasSideEffects()
        is ArrayAccessExpr -> name.hasSideEffects() || index.hasSideEffects()
        is FieldAccessExpr -> scope.hasSideEffects()
        // TODO(gpe): more cases
        else -> true
    }
}

public fun BinaryExpr.Operator.flip(): BinaryExpr.Operator? {
    return when (this) {
        BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MULTIPLY -> this
        BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> this
        BinaryExpr.Operator.BINARY_AND, BinaryExpr.Operator.BINARY_OR -> this
        BinaryExpr.Operator.XOR, BinaryExpr.Operator.OR, BinaryExpr.Operator.AND -> this
        BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
        BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
        BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER
        BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER_EQUALS
        else -> null
    }
}
