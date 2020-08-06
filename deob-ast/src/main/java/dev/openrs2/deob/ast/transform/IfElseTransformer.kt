package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.type.VoidType
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.countNots
import dev.openrs2.deob.ast.util.not
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class IfElseTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        var oldUnit: CompilationUnit
        do {
            oldUnit = unit.clone()
            transform(unit)
        } while (unit != oldUnit)
    }

    private fun transform(unit: CompilationUnit) {
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
         *         throw ...; // or return
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
         *     throw ...; // or return
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

        /*
         * Rewrite:
         *
         * if (a) {
         *     if (b) {
         *         ...
         *     }
         * }
         *
         * to:
         *
         * if (a && b) {
         *     ...
         * }
         */
        unit.walk { outerStmt: IfStmt ->
            if (outerStmt.elseStmt.isPresent) {
                return@walk
            }

            val innerStmt = outerStmt.thenStmt.getIf() ?: return@walk
            if (innerStmt.elseStmt.isPresent) {
                return@walk
            }

            outerStmt.condition = BinaryExpr(outerStmt.condition, innerStmt.condition, BinaryExpr.Operator.AND)
            outerStmt.thenStmt = innerStmt.thenStmt
        }

        unit.walk { method: MethodDeclaration ->
            if (method.type !is VoidType) {
                return@walk
            }

            method.body.ifPresent { body ->
                val ifStmt = body.statements.lastOrNull() ?: return@ifPresent
                if (ifStmt !is IfStmt) {
                    return@ifPresent
                }

                val thenStatements = ifStmt.thenStmt.findAll(Statement::class.java).size
                ifStmt.elseStmt.ifPresentOrElse({ elseStmt ->
                    if (elseStmt.isIf()) {
                        return@ifPresentOrElse
                    }

                    val elseStatements = elseStmt.findAll(Statement::class.java).size
                    if (thenStatements <= IF_DEINDENT_THRESHOLD && elseStatements <= IF_DEINDENT_THRESHOLD) {
                        return@ifPresentOrElse
                    }

                    /*
                     * Rewrite:
                     *
                     * void m(...) {
                     *     ...
                     *     if (a) {
                     *         ...
                     *     } else {
                     *         ...
                     *     }
                     * }
                     *
                     * to:
                     *
                     * void m(...) {
                     *     ...
                     *     if (!a) { // or `if (a)`, depending on which arm is smaller
                     *         ...
                     *         return;
                     *     }
                     *     ...
                     * }
                     */
                    if (elseStatements > thenStatements) {
                        body.statements.addAll(elseStmt.flatten())

                        ifStmt.thenStmt = ifStmt.thenStmt.appendReturn()
                        ifStmt.removeElseStmt()
                    } else {
                        body.statements.addAll(ifStmt.thenStmt.flatten())

                        ifStmt.condition = ifStmt.condition.not()
                        ifStmt.thenStmt = elseStmt.appendReturn()
                        ifStmt.removeElseStmt()
                    }
                }, {
                    /*
                     * Rewrite:
                     *
                     * void m(...) {
                     *     ...
                     *     if (a) {
                     *         ...
                     *     }
                     * }
                     *
                     * to:
                     *
                     * void m(...) {
                     *     ...
                     *     if (!a) {
                     *         return;
                     *     }
                     *     ...
                     * }
                     */
                    if (thenStatements <= IF_DEINDENT_THRESHOLD) {
                        return@ifPresentOrElse
                    }

                    body.statements.addAll(ifStmt.thenStmt.flatten())

                    ifStmt.condition = ifStmt.condition.not()
                    ifStmt.thenStmt = BlockStmt(NodeList(ReturnStmt()))
                })
            }
        }

        /*
         * Rewrite:
         *
         * if (a) {
         *     ...
         *     throw ...; // or return
         * } else {
         *     ...
         * }
         *
         * to:
         *
         * if (a) { // or `if (!a)`, if the arms are swapped
         *     ...
         *     throw ...; // or return
         * }
         * ...
         */
        unit.walk { blockStmt: BlockStmt ->
            /*
             * XXX(gpe): need to iterate through blockStmt.stmts manually as we
             * insert extra statements during iteration (ugh!)
             */
            var index = 0
            while (index < blockStmt.statements.size) {
                val ifStmt = blockStmt.statements[index]
                if (ifStmt !is IfStmt) {
                    index++
                    continue
                }

                ifStmt.elseStmt.ifPresent { elseStmt ->
                    if (elseStmt.isIf()) {
                        return@ifPresent
                    }

                    /*
                     * If one of the arms consists of just a throw, move that
                     * into an if regardless of the fact that the method as a
                     * whole will end up longer.
                     */
                    if (ifStmt.thenStmt.isThrow()) {
                        blockStmt.statements.addAll(index + 1, elseStmt.flatten())

                        ifStmt.removeElseStmt()

                        return@ifPresent
                    } else if (elseStmt.isThrow()) {
                        blockStmt.statements.addAll(index + 1, ifStmt.thenStmt.flatten())

                        ifStmt.condition = ifStmt.condition.not()
                        ifStmt.thenStmt = elseStmt.appendReturn()
                        ifStmt.removeElseStmt()

                        return@ifPresent
                    }

                    val thenStatements = ifStmt.thenStmt.findAll(Statement::class.java).size
                    val elseStatements = elseStmt.findAll(Statement::class.java).size
                    if (thenStatements <= IF_DEINDENT_THRESHOLD && elseStatements <= IF_DEINDENT_THRESHOLD) {
                        return@ifPresent
                    }

                    if (elseStatements > thenStatements && ifStmt.thenStmt.isTailThrowOrReturn()) {
                        blockStmt.statements.addAll(index + 1, elseStmt.flatten())

                        ifStmt.removeElseStmt()
                    } else if (elseStmt.isTailThrowOrReturn()) {
                        blockStmt.statements.addAll(index + 1, ifStmt.thenStmt.flatten())

                        ifStmt.condition = ifStmt.condition.not()
                        ifStmt.thenStmt = elseStmt.appendReturn()
                        ifStmt.removeElseStmt()
                    }
                }

                index++
            }
        }
    }

    private fun Statement.appendReturn(): Statement {
        return if (this is BlockStmt) {
            val last = statements.lastOrNull()
            if (last is ReturnStmt || last is ThrowStmt) {
                clone()
            } else {
                BlockStmt(NodeList(statements.map(Statement::clone).plus(ReturnStmt())))
            }
        } else if (this is ReturnStmt || this is ThrowStmt) {
            clone()
        } else {
            BlockStmt(NodeList(clone(), ReturnStmt()))
        }
    }

    private fun Statement.flatten(): Collection<Statement> {
        return if (this is BlockStmt) {
            statements.map(Statement::clone)
        } else {
            listOf(clone())
        }
    }

    private fun Statement.isIf(): Boolean {
        return getIf() != null
    }

    private fun Statement.getIf(): IfStmt? {
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

    private fun Statement.isThrow(): Boolean {
        return when (this) {
            is ThrowStmt -> true
            is BlockStmt -> statements.singleOrNull() is ThrowStmt
            else -> false
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

    private companion object {
        private const val IF_DEINDENT_THRESHOLD = 5
    }
}
