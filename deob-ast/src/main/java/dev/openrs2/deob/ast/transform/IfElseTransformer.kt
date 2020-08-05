package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.countNots
import dev.openrs2.deob.ast.util.not
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class IfElseTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt: Statement ->
                val condition = stmt.condition
                val thenStmt = stmt.thenStmt
                if (thenStmt.isIf() && !elseStmt.isIf()) {
                    /*
                     * Rewrite:
                     *
                     * if (a) {
                     *     if (b) {
                     *         ...
                     *     }
                     * } else {
                     *     ...
                     * }
                     *
                     * to:
                     *
                     * if (!a) {
                     *     ...
                     * } else {
                     *     if (b) {
                     *         ...
                     *     }
                     * }
                     */
                    stmt.condition = condition.not()
                    stmt.thenStmt = elseStmt.clone()
                    stmt.setElseStmt(thenStmt.clone())
                } else if (!thenStmt.isIf() && elseStmt.isIf()) {
                    /*
                     * Don't consider any more conditions for swapping the
                     * if/else branches, as it'll introduce another level of
                     * indentation.
                     */
                    return@ifPresent
                }

                /*
                 * Prefer fewer NOTs in the if condition
                 *
                 * Rewrites:
                 *
                 * if (!a) {
                 *     ...
                 * } else {
                 *     ....
                 * }
                 *
                 * to:
                 *
                 * if (a) {
                 *     ...
                 * } else {
                 *     ...
                 * }
                 *
                 */
                val notCondition = condition.not()
                if (notCondition.countNots() < condition.countNots()) {
                    stmt.condition = notCondition
                    if (elseStmt is IfStmt) {
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

        /*
         * Rewrite:
         *
         * } else {
         *     if (a) {
         *         ...
         *     }
         * }
         *
         * to:
         *
         * } else if (a) {
         *     ....
         * }
         */
        unit.walk { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt ->
                val ifStmt = elseStmt.getIf()
                if (ifStmt != null) {
                    stmt.setElseStmt(ifStmt)
                }
            }
        }

        /*
         * Rewrite:
         *
         * } else {
         *     if (!a) {
         *         ...
         *         throw ...;
         *     }
         *     ...
         * }
         *
         * to:
         *
         * } else if (a) {
         *     ...
         * } else {
         *     ...
         *     throw ...;
         * }
         */
        unit.walk { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt ->
                // match
                if (elseStmt !is BlockStmt) {
                    return@ifPresent
                }

                val statements = elseStmt.statements
                if (statements.isEmpty()) {
                    return@ifPresent
                }

                val ifStmt = statements[0]
                if (ifStmt !is IfStmt) {
                    return@ifPresent
                } else if (ifStmt.elseStmt.isPresent) {
                    return@ifPresent
                }

                val thenStmt = ifStmt.thenStmt
                if (!thenStmt.isTailThrowOrReturn()) {
                    return@ifPresent
                }

                // rewrite
                val condition = ifStmt.condition.not()

                val tail = elseStmt.clone()
                tail.statements.removeAt(0)

                elseStmt.replace(IfStmt(condition, tail, thenStmt.clone()))
            }
        }

        /**
         * Rewrite:
         *
         * } else {
         *     return a ? ... : ...;
         * }
         *
         * to:
         *
         * } else if (a) {
         *     return ...;
         * } else {
         *     return ...;
         * }
         */
        unit.walk { stmt: IfStmt ->
            stmt.elseStmt.ifPresent { elseStmt ->
                // match
                if (elseStmt !is BlockStmt) {
                    return@ifPresent
                }

                val head = elseStmt.statements.singleOrNull() ?: return@ifPresent
                if (head !is ReturnStmt) {
                    return@ifPresent
                }

                head.expression.ifPresent { expr ->
                    if (expr !is ConditionalExpr) {
                        return@ifPresent
                    }

                    // replace
                    val thenBlock = BlockStmt(NodeList(ReturnStmt(expr.thenExpr)))
                    val elseBlock = BlockStmt(NodeList(ReturnStmt(expr.elseExpr)))
                    stmt.setElseStmt(IfStmt(expr.condition, thenBlock, elseBlock))
                }
            }
        }
    }

    private fun Statement.isIf(): Boolean {
        return getIf() != null
    }

    private fun Statement.getIf(): Statement? {
        return when (this) {
            is IfStmt -> clone()
            is BlockStmt -> {
                val head = statements.singleOrNull()
                if (head is IfStmt) {
                    head.clone()
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun Statement.isTailThrowOrReturn(): Boolean {
        return when (this) {
            is ThrowStmt, is ReturnStmt -> true
            is BlockStmt -> {
                val tail = statements.lastOrNull()
                tail is ThrowStmt || tail is ReturnStmt
            }
            else -> false
        }
    }
}
