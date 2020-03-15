package dev.openrs2.deob.ir

sealed class Stmt {
    fun accept(visitor: StmtVisitor) = when (this) {
        is AssignmentStmt -> visitor.visitAssignmen(slot, expr)
        is CallStmt -> visitor.visitCall(expr)
        is IfStmt -> visitor.visitIf(expr)
        is ReturnStmt -> visitor.visitReturn(expr)
    }
}

data class AssignmentStmt(val slot: VarExpr, val expr: Expr) : Stmt()
data class CallStmt(val expr: CallExpr) : Stmt()
data class IfStmt(val expr: BinaryExpr) : Stmt()
data class ReturnStmt(val expr: Expr?) : Stmt()

interface StmtVisitor {
    fun visitAssignmen(variable: VarExpr, value: Expr)
    fun visitCall(expr: CallExpr)
    fun visitIf(conditional: BinaryExpr)
    fun visitReturn(value: Expr?)
}
