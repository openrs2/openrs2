package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.walk

class IfElseTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt: Statement ->
                val condition = stmt.condition
                val thenStmt = stmt.thenStmt
                if (isIf(thenStmt) && !isIf(elseStmt)) {
                    stmt.condition = ExprUtils.not(condition)
                    stmt.thenStmt = elseStmt.clone()
                    stmt.setElseStmt(thenStmt.clone())
                } else if (!isIf(thenStmt) && isIf(elseStmt)) {
                    /*
                     * Don't consider any more conditions for swapping the
                     * if/else branches, as it'll introduce another level of
                     * indentation.
                     */
                    return@ifPresent
                }

                // Prefer fewer NOTs in the if condition
                val notCondition = ExprUtils.not(condition)
                if (ExprUtils.countNots(notCondition) < ExprUtils.countNots(condition)) {
                    stmt.condition = notCondition
                    if (elseStmt.isIfStmt) {
                        val block = BlockStmt()
                        block.statements.add(elseStmt.clone())
                        stmt.thenStmt = block
                    } else {
                        stmt.thenStmt = elseStmt.clone()
                    }
                    stmt.setElseStmt(thenStmt.clone())
                }
            }
        }

        unit.walk(Node.TreeTraversal.POSTORDER) { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt ->
                if (isIf(elseStmt)) {
                    stmt.setElseStmt(getIf(elseStmt))
                }
            }
        }

        /*
         * Rewrite:
         *
         * ...
         * } else {
         *     if (x != 123) {
         *         ...
         *         throw ...;
         *     }
         *     ...
         * }
         *
         * to:
         *
         * ...
         * } else if (x == 123) {
         *     ...
         * } else {
         *     ...
         *     throw ...;
         * }
         */
        unit.walk(Node.TreeTraversal.POSTORDER) { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt ->
                // match
                if (!elseStmt.isBlockStmt) {
                    return@ifPresent
                }

                val blockStmt = elseStmt.asBlockStmt()
                val statements = blockStmt.statements
                if (statements.isEmpty()) {
                    return@ifPresent
                }

                val head = statements[0]
                if (!head.isIfStmt) {
                    return@ifPresent
                }

                val ifStmt = head.asIfStmt()
                if (ifStmt.elseStmt.isPresent) {
                    return@ifPresent
                }

                val thenStmt = ifStmt.thenStmt
                if (!isTailThrowOrReturn(thenStmt)) {
                    return@ifPresent
                }

                // rewrite
                val condition = ExprUtils.not(ifStmt.condition)

                val tail = blockStmt.clone()
                tail.statements.removeAt(0)

                elseStmt.replace(IfStmt(condition, tail, thenStmt.clone()))
            }
        }
    }

    companion object {
        private fun isIf(stmt: Statement): Boolean {
            return if (stmt.isIfStmt) {
                true
            } else if (stmt.isBlockStmt) {
                val stmts = stmt.asBlockStmt().statements
                stmts.size == 1 && stmts[0].isIfStmt
            } else {
                false
            }
        }

        private fun getIf(stmt: Statement): Statement {
            if (stmt.isIfStmt) {
                return stmt.clone()
            } else if (stmt.isBlockStmt) {
                val stmts = stmt.asBlockStmt().statements
                if (stmts.size == 1) {
                    val head = stmts[0]
                    if (head.isIfStmt) {
                        return head.clone()
                    }
                }
            }
            throw IllegalArgumentException()
        }

        private fun isTailThrowOrReturn(stmt: Statement): Boolean {
            return if (stmt.isThrowStmt || stmt.isReturnStmt) {
                true
            } else if (stmt.isBlockStmt) {
                val stmts = stmt.asBlockStmt().statements
                if (stmts.isEmpty()) {
                    return false
                }

                val tail = stmts[stmts.size - 1]
                tail.isThrowStmt || tail.isReturnStmt
            } else {
                false
            }
        }
    }
}
