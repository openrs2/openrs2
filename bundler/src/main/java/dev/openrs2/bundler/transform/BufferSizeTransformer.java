package dev.openrs2.bundler.transform;

import java.util.Optional;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.InsnNodeUtils;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.MethodNodeUtilsKt;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BufferSizeTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(BufferSizeTransformer.class);

	private static final InsnMatcher GPP1_POS_MATCHER = InsnMatcher.compile("LDC (INVOKESPECIAL | INVOKEVIRTUAL) GETSTATIC");
	private static final InsnMatcher NEW_BUFFER_MATCHER = InsnMatcher.compile("NEW DUP (SIPUSH | LDC) INVOKESPECIAL PUTSTATIC");

	private static Optional<MemberRef> findBuffer(MethodNode method) {
		return GPP1_POS_MATCHER.match(method)
			.filter(match -> {
				var ldc = (LdcInsnNode) match.get(0);
				return ldc.cst.equals("gpp1 pos:");
			})
			.map(match -> {
				var getstatic = (FieldInsnNode) match.get(2);
				return new MemberRef(getstatic);
			})
			.findAny();
	}

	private MemberRef buffer;
	private int buffersResized;

	@Override
	protected void preTransform(ClassPath classPath) {
		buffer = null;
		buffersResized = 0;

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				for (var method : clazz.methods) {
					if (!MethodNodeUtilsKt.hasCode(method)) {
						continue;
					}

					var buffer = findBuffer(method);
					if (buffer.isEmpty()) {
						continue;
					}

					this.buffer = buffer.orElseThrow();
					logger.info("Identified buffer: {}", this.buffer);
					break;
				}
			}
		}
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		if (buffer == null) {
			return false;
		}

		NEW_BUFFER_MATCHER.match(method).forEach(match -> {
			var putstatic = (FieldInsnNode) match.get(4);
			if (!putstatic.owner.equals(buffer.getOwner())) {
				return;
			} else if (!putstatic.name.equals(buffer.getName())) {
				return;
			} else if (!putstatic.desc.equals(buffer.getDesc())) {
				return;
			}

			method.instructions.set(match.get(2), InsnNodeUtils.createIntConstant(65535));
			buffersResized++;
		});

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Resized {} buffers to 65535 bytes", buffersResized);
	}
}
