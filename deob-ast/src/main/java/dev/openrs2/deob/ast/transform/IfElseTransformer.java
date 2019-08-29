package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import dev.openrs2.deob.ast.util.ExprUtils;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class IfElseTransformer extends Transformer {
	private static boolean isIf(Statement stmt) {
		if (stmt.isIfStmt()) {
			return true;
		} else if (stmt.isBlockStmt()) {
			var stmts = stmt.asBlockStmt().getStatements();
			return stmts.size() == 1 && stmts.get(0).isIfStmt();
		} else {
			return false;
		}
	}

	private static Statement getIf(Statement stmt) {
		if (stmt.isIfStmt()) {
			return stmt.clone();
		} else if (stmt.isBlockStmt()) {
			var stmts = stmt.asBlockStmt().getStatements();
			if (stmts.size() == 1) {
				Statement head = stmts.get(0);
				if (head.isIfStmt()) {
					return head.clone();
				}
			}
		}

		throw new IllegalArgumentException();
	}

	private static boolean isTailThrowOrReturn(Statement stmt) {
		if (stmt.isThrowStmt() || stmt.isReturnStmt()) {
			return true;
		} else if (stmt.isBlockStmt()) {
			var stmts = stmt.asBlockStmt().getStatements();
			if (stmts.isEmpty()) {
				return false;
			}

			var tail = stmts.get(stmts.size() - 1);
			return tail.isThrowStmt() || tail.isReturnStmt();
		} else {
			return false;
		}
	}

	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, IfStmt.class, stmt -> {
			stmt.getElseStmt().ifPresent(elseStmt -> {
				var condition = stmt.getCondition();
				var thenStmt = stmt.getThenStmt();
				if (isIf(thenStmt) && !isIf(elseStmt)) {
					stmt.setCondition(ExprUtils.not(condition));
					stmt.setThenStmt(elseStmt.clone());
					stmt.setElseStmt(thenStmt.clone());
				} else if (!isIf(thenStmt) && isIf(elseStmt)) {
					/*
					 * Don't consider any more conditions for swapping the
					 * if/else branches, as it'll introduce another level of
					 * indentation.
					 */
					return;
				}

				/* Prefer fewer NOTs in the if condition. */
				var notCondition = ExprUtils.not(condition);
				if (ExprUtils.countNots(notCondition) < ExprUtils.countNots(condition)) {
					stmt.setCondition(notCondition);
					if (elseStmt.isIfStmt()) {
						var block = new BlockStmt();
						block.getStatements().add(elseStmt.clone());
						stmt.setThenStmt(block);
					} else {
						stmt.setThenStmt(elseStmt.clone());
					}
					stmt.setElseStmt(thenStmt.clone());
				}
			});
		});

		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, IfStmt.class, stmt -> {
			stmt.getElseStmt().ifPresent(elseStmt -> {
				if (isIf(elseStmt)) {
					stmt.setElseStmt(getIf(elseStmt));
				}
			});
		});

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
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, IfStmt.class, stmt -> {
			stmt.getElseStmt().ifPresent(elseStmt -> {
				/* match */
				if (!elseStmt.isBlockStmt()) {
					return;
				}

				var blockStmt = elseStmt.asBlockStmt();
				var statements = blockStmt.getStatements();
				if (statements.isEmpty()) {
					return;
				}

				var head = statements.get(0);
				if (!head.isIfStmt()) {
					return;
				}

				var ifStmt = head.asIfStmt();
				if (ifStmt.getElseStmt().isPresent()) {
					return;
				}

				var thenStmt = ifStmt.getThenStmt();
				if (!isTailThrowOrReturn(thenStmt)) {
					return;
				}

				/* rewrite */
				var condition = ExprUtils.not(ifStmt.getCondition());

				var tail = blockStmt.clone();
				tail.getStatements().remove(0);

				elseStmt.replace(new IfStmt(condition, tail, thenStmt.clone()));
			});
		});
	}
}
