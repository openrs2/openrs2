package dev.openrs2.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;

public final class InsnMatcher {
	private static final int PRIVATE_USE_AREA = 0xE000;
	private static final ImmutableMap<String, int[]> OPCODE_GROUPS = ImmutableMap.<String, int[]>builder()
		.put("InsnNode", new int[] {
			Opcodes.NOP,
			Opcodes.ACONST_NULL,
			Opcodes.ICONST_M1,
			Opcodes.ICONST_0,
			Opcodes.ICONST_1,
			Opcodes.ICONST_2,
			Opcodes.ICONST_3,
			Opcodes.ICONST_4,
			Opcodes.ICONST_5,
			Opcodes.LCONST_0,
			Opcodes.LCONST_1,
			Opcodes.FCONST_0,
			Opcodes.FCONST_1,
			Opcodes.FCONST_2,
			Opcodes.DCONST_0,
			Opcodes.DCONST_1,
			Opcodes.IALOAD,
			Opcodes.LALOAD,
			Opcodes.FALOAD,
			Opcodes.DALOAD,
			Opcodes.AALOAD,
			Opcodes.BALOAD,
			Opcodes.CALOAD,
			Opcodes.SALOAD,
			Opcodes.IASTORE,
			Opcodes.LASTORE,
			Opcodes.FASTORE,
			Opcodes.DASTORE,
			Opcodes.AASTORE,
			Opcodes.BASTORE,
			Opcodes.CASTORE,
			Opcodes.SASTORE,
			Opcodes.POP,
			Opcodes.POP2,
			Opcodes.DUP,
			Opcodes.DUP_X1,
			Opcodes.DUP_X2,
			Opcodes.DUP2,
			Opcodes.DUP2_X1,
			Opcodes.DUP2_X2,
			Opcodes.SWAP,
			Opcodes.IADD,
			Opcodes.LADD,
			Opcodes.FADD,
			Opcodes.DADD,
			Opcodes.ISUB,
			Opcodes.LSUB,
			Opcodes.FSUB,
			Opcodes.DSUB,
			Opcodes.IMUL,
			Opcodes.LMUL,
			Opcodes.FMUL,
			Opcodes.DMUL,
			Opcodes.IDIV,
			Opcodes.LDIV,
			Opcodes.FDIV,
			Opcodes.DDIV,
			Opcodes.IREM,
			Opcodes.LREM,
			Opcodes.FREM,
			Opcodes.DREM,
			Opcodes.INEG,
			Opcodes.LNEG,
			Opcodes.FNEG,
			Opcodes.DNEG,
			Opcodes.ISHL,
			Opcodes.LSHL,
			Opcodes.ISHR,
			Opcodes.LSHR,
			Opcodes.IUSHR,
			Opcodes.LUSHR,
			Opcodes.IAND,
			Opcodes.LAND,
			Opcodes.IOR,
			Opcodes.LOR,
			Opcodes.IXOR,
			Opcodes.LXOR,
			Opcodes.I2L,
			Opcodes.I2F,
			Opcodes.I2D,
			Opcodes.L2I,
			Opcodes.L2F,
			Opcodes.L2D,
			Opcodes.F2I,
			Opcodes.F2L,
			Opcodes.F2D,
			Opcodes.D2I,
			Opcodes.D2L,
			Opcodes.D2F,
			Opcodes.I2B,
			Opcodes.I2C,
			Opcodes.I2S,
			Opcodes.LCMP,
			Opcodes.FCMPL,
			Opcodes.FCMPG,
			Opcodes.DCMPL,
			Opcodes.DCMPG,
			Opcodes.IRETURN,
			Opcodes.LRETURN,
			Opcodes.FRETURN,
			Opcodes.DRETURN,
			Opcodes.ARETURN,
			Opcodes.RETURN,
			Opcodes.ARRAYLENGTH,
			Opcodes.ATHROW,
			Opcodes.MONITORENTER,
			Opcodes.MONITOREXIT
		})
		.put("IntInsnNode", new int[] {
			Opcodes.BIPUSH,
			Opcodes.SIPUSH,
			Opcodes.NEWARRAY
		})
		.put("VarInsnNode", new int[] {
			Opcodes.ILOAD,
			Opcodes.LLOAD,
			Opcodes.FLOAD,
			Opcodes.DLOAD,
			Opcodes.ALOAD,
			Opcodes.ISTORE,
			Opcodes.LSTORE,
			Opcodes.FSTORE,
			Opcodes.DSTORE,
			Opcodes.ASTORE,
			Opcodes.RET
		})
		.put("TypeInsnNode", new int[] {
			Opcodes.NEW,
			Opcodes.ANEWARRAY,
			Opcodes.CHECKCAST,
			Opcodes.INSTANCEOF
		})
		.put("FieldInsnNode", new int[] {
			Opcodes.GETSTATIC,
			Opcodes.PUTSTATIC,
			Opcodes.GETFIELD,
			Opcodes.PUTFIELD
		})
		.put("MethodInsnNode", new int[] {
			Opcodes.INVOKEVIRTUAL,
			Opcodes.INVOKESPECIAL,
			Opcodes.INVOKESTATIC,
			Opcodes.INVOKEINTERFACE
		})
		.put("InvokeDynamicInsnNode", new int[] {
			Opcodes.INVOKEDYNAMIC
		})
		.put("JumpInsnNode", new int[] {
			Opcodes.IFEQ,
			Opcodes.IFNE,
			Opcodes.IFLT,
			Opcodes.IFGE,
			Opcodes.IFGT,
			Opcodes.IFLE,
			Opcodes.IF_ICMPEQ,
			Opcodes.IF_ICMPNE,
			Opcodes.IF_ICMPLT,
			Opcodes.IF_ICMPGE,
			Opcodes.IF_ICMPGT,
			Opcodes.IF_ICMPLE,
			Opcodes.IF_ACMPEQ,
			Opcodes.IF_ACMPNE,
			Opcodes.GOTO,
			Opcodes.JSR,
			Opcodes.IFNULL,
			Opcodes.IFNONNULL
		})
		.put("LdcInsnNode", new int[] {
			Opcodes.LDC
		})
		.put("IincInsnNode", new int[] {
			Opcodes.IINC
		})
		.put("TableSwitchInsnNode", new int[] {
			Opcodes.TABLESWITCH
		})
		.put("LookupSwitchInsnNode", new int[] {
			Opcodes.LOOKUPSWITCH
		})
		.put("MultiANewArrayInsnNode", new int[] {
			Opcodes.MULTIANEWARRAY
		})
		.put("ICONST", new int[] {
			Opcodes.ICONST_M1,
			Opcodes.ICONST_0,
			Opcodes.ICONST_1,
			Opcodes.ICONST_2,
			Opcodes.ICONST_3,
			Opcodes.ICONST_4,
			Opcodes.ICONST_5
		})
		.build();

	private static char opcodeToCodepoint(int opcode) {
		return (char) (PRIVATE_USE_AREA + opcode);
	}

	private static List<AbstractInsnNode> createRealInsnList(InsnList list) {
		List<AbstractInsnNode> realInsns = new ArrayList<>();
		for (var it = list.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (insn.getOpcode() != -1) {
				realInsns.add(insn);
			}
		}
		return realInsns;
	}

	private static String createCodepointSeq(List<AbstractInsnNode> insns) {
		var codepoints = new char[insns.size()];
		for (var i = 0; i < codepoints.length; i++) {
			codepoints[i] = opcodeToCodepoint(insns.get(i).getOpcode());
		}
		return new String(codepoints);
	}

	public static InsnMatcher compile(String expr) {
		var pattern = new StringBuilder();
		var opcode = new StringBuilder();

		for (var i = 0; i < expr.length(); i++) {
			var c = expr.charAt(i);
			if (Character.isLetterOrDigit(c) || c == '_') {
				opcode.append(c);
			} else {
				if (opcode.length() > 0) {
					appendOpcodeRegex(pattern, opcode.toString());
					opcode.delete(0, opcode.length());
				}

				if (!Character.isWhitespace(c)) {
					pattern.append(c);
				}
			}
		}

		if (opcode.length() > 0) {
			appendOpcodeRegex(pattern, opcode.toString());
		}

		return new InsnMatcher(Pattern.compile(pattern.toString()));
	}

	private static void appendOpcodeRegex(StringBuilder pattern, String opcode) {
		for (var i = 0; i < Printer.OPCODES.length; i++) {
			if (opcode.equals(Printer.OPCODES[i])) {
				pattern.append(opcodeToCodepoint(i));
				return;
			}
		}

		var group = OPCODE_GROUPS.get(opcode);
		if (group != null) {
			pattern.append('(');
			for (var i = 0; i < group.length; i++) {
				pattern.append(opcodeToCodepoint(group[i]));
				if (i != group.length - 1) {
					pattern.append('|');
				}
			}
			pattern.append(')');
			return;
		}

		if (opcode.equals("AbstractInsnNode")) {
			pattern.append('.');
			return;
		}

		throw new IllegalArgumentException(opcode + " is not a valid opcode or opcode group");
	}

	private final Pattern pattern;

	private InsnMatcher(Pattern pattern) {
		this.pattern = pattern;
	}

	public Stream<ImmutableList<AbstractInsnNode>> match(MethodNode method) {
		return match(method.instructions);
	}

	public Stream<ImmutableList<AbstractInsnNode>> match(InsnList list) {
		Stream.Builder<ImmutableList<AbstractInsnNode>> matches = Stream.builder();

		var insns = createRealInsnList(list);
		var matcher = pattern.matcher(createCodepointSeq(insns));
		while (matcher.find()) {
			var start = matcher.start();
			var end = matcher.end();
			matches.add(ImmutableList.copyOf(insns.subList(start, end)));
		}

		return matches.build();
	}
}
