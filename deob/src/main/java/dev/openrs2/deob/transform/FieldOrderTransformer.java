package dev.openrs2.deob.transform;

import java.util.HashMap;

import dev.openrs2.asm.MemberDesc;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;

public final class FieldOrderTransformer extends Transformer {
	private static final String CONSTRUCTOR = "<init>";
	private static final String STATIC_CONSTRUCTOR = "<clinit>";

	private static void sortFields(ClassNode clazz, String ctorName, int opcode) {
		clazz.methods.stream()
			.filter(m -> m.name.equals(ctorName))
			.findFirst()
			.ifPresent(ctor -> {
				var fields = new HashMap<MemberDesc, Integer>();
				var index = 0;

				for (var it = ctor.instructions.iterator(); it.hasNext(); ) {
					var insn = it.next();
					if (insn.getOpcode() != opcode) {
						continue;
					}

					var putfield = (FieldInsnNode) insn;
					if (!putfield.owner.equals(clazz.name)) {
						continue;
					}

					var desc = new MemberDesc(putfield);
					if (!fields.containsKey(desc)) {
						fields.put(desc, index++);
					}
				}

				clazz.fields.sort((a, b) -> {
					var indexA = fields.getOrDefault(new MemberDesc(a), -1);
					var indexB = fields.getOrDefault(new MemberDesc(b), -1);
					return Integer.compare(indexA, indexB);
				});
			});
	}

	@Override
	public boolean transformClass(ClassPath classPath, Library library, ClassNode clazz) {
		sortFields(clazz, CONSTRUCTOR, Opcodes.PUTFIELD);
		sortFields(clazz, STATIC_CONSTRUCTOR, Opcodes.PUTSTATIC);
		return false;
	}
}
