package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import dev.openrs2.deob.ast.util.ExprUtils;

public final class IfElseTransformer extends Transformer {
	private static boolean isIf(Statement stmt) {
		if (stmt.isIfStmt()) {
			return true;
		} else if (stmt.isBlockStmt()) {
			NodeList<Statement> stmts = stmt.asBlockStmt().getStatements();
			return stmts.size() == 1 && stmts.get(0).isIfStmt();
		} else {
			return false;
		}
	}

	private static Statement getIf(Statement stmt) {
		if (stmt.isIfStmt()) {
			return stmt.clone();
		} else if (stmt.isBlockStmt()) {
			NodeList<Statement> stmts = stmt.asBlockStmt().getStatements();
			if (stmts.size() == 1) {
				Statement head = stmts.get(0);
				if (head.isIfStmt()) {
					return head.clone();
				}
			}
		}

		throw new IllegalArgumentException();
	}

	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(IfStmt.class).forEach(stmt -> {
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

				/*
				 * Prefer fewer NOTs in the if condition.
				 */
				var notCondition = ExprUtils.not(condition);
				if (ExprUtils.countNots(notCondition) < ExprUtils.countNots(condition)) {
					stmt.setCondition(notCondition);
					stmt.setThenStmt(elseStmt.clone());
					stmt.setElseStmt(thenStmt.clone());
				}
			});
		});

		unit.findAll(IfStmt.class).forEach(stmt -> {
			stmt.getElseStmt().ifPresent(elseStmt -> {
				if (isIf(elseStmt)) {
					stmt.setElseStmt(getIf(elseStmt));
				}
			});
		});
	}
}
