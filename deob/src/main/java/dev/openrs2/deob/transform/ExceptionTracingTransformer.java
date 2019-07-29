package dev.openrs2.deob.transform;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.InsnNodeUtils;
import dev.openrs2.asm.Library;
import dev.openrs2.asm.Transformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionTracingTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(ExceptionTracingTransformer.class);

	private static final InsnMatcher CATCH_MATCHER = InsnMatcher.compile("ASTORE ALOAD (| LDC INVOKESTATIC | NEW DUP (LDC INVOKESPECIAL | INVOKESPECIAL LDC INVOKEVIRTUAL) ((ILOAD | LLOAD | FLOAD | DLOAD | (ALOAD IFNULL LDC GOTO LDC) | BIPUSH) INVOKEVIRTUAL)* INVOKEVIRTUAL INVOKESTATIC) ATHROW");

	private int tracingTryCatches;

	@Override
	public void preTransform(Library library) {
		tracingTryCatches = 0;
	}

	@Override
	public void transformCode(ClassNode clazz, MethodNode method) {
		CATCH_MATCHER.match(method).forEach(match -> {
			var foundTryCatch = method.tryCatchBlocks.removeIf(tryCatch -> {
				if (!"java/lang/RuntimeException".equals(tryCatch.type)) {
					return false;
				}
				return InsnNodeUtils.nextReal(tryCatch.handler) == match.get(0);
			});

			if (foundTryCatch) {
				match.forEach(method.instructions::remove);
				tracingTryCatches++;
			}
		});
	}

	@Override
	public void postTransform(Library library) {
		logger.info("Removed {} tracing try/catch blocks", tracingTryCatches);
	}
}
