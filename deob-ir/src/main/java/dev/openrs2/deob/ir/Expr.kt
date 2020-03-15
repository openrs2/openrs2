package dev.openrs2.deob.ir

import dev.openrs2.asm.MemberRef
import org.objectweb.asm.Type

sealed class Storage

class LocalVarStorage(val slot: Int = -1) : Storage()
class FieldStorage(val instance: Expr, val member: MemberRef) : Storage()
class StaticStorage(val member: MemberRef) : Storage()

sealed class UnaryOp {
    object Negate : UnaryOp()
    data class InstanceOf(val type: Type) : UnaryOp()
    data class Cast(val type: Type) : UnaryOp()
}

enum class BinOp {
    Equals,
    NotEquals,
    LessThan,
    GreaterThanOrEquals,
    GreaterThan,
    LessThanOrEquals,
    Remainder,
    Divide,
    Multiply,
    Add,
    Subtract,
    ShiftLeft,
    ShiftRight,
    UnsignedShiftRight,
    ExclusiveOr
}

data class BinaryExpr(val lhs: Expr, val rhs: Expr, val op: dev.openrs2.deob.ir.BinOp) : Expr()

sealed class Expr

data class IndexExpr(val array: Expr, val index: Expr) : Expr()

data class UnaryExpr(val operand: Expr, val operator: UnaryOp) : Expr()

data class ConstExpr(val value: Any?) : Expr()

data class CallExpr(val instance: Expr?, val method: MemberRef, val arguments: List<Expr>) : Expr()

data class VarExpr(val storage: Storage) : Expr() {
    companion object {
        fun local(slot: Int) = VarExpr(LocalVarStorage(slot))
    }
}
