package dev.openrs2.deob.analysis;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import dev.openrs2.asm.InsnNodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;

public final class IntInterpreter extends Interpreter<IntValue> {
	// TODO(gpe): this is fairly arbitrary and will need tweaking
	private static final int MAX_TRACKED_VALUES = 8;

	private final Interpreter<BasicValue> basicInterpreter = new BasicInterpreter();
	private final ImmutableSet<Integer>[] parameters;

	public IntInterpreter(ImmutableSet<Integer>[] parameters) {
		super(Opcodes.ASM7);
		this.parameters = parameters;
	}

	@Override
	public IntValue newValue(Type type) {
		var basicValue = basicInterpreter.newValue(type);
		if (basicValue == null) {
			return null;
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public IntValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
		var basicValue = basicInterpreter.newParameterValue(isInstanceMethod, local, type);
		if (basicValue == null) {
			return null;
		}

		if (parameters != null) {
			int parameterIndex;
			if (isInstanceMethod) {
				if (local == 0) {
					return IntValue.newUnknown(basicValue);
				}
				parameterIndex = local - 1;
			} else {
				parameterIndex = local;
			}

			var parameter = parameters[parameterIndex];
			if (parameter != null) {
				return IntValue.newConstant(basicValue, parameter);
			}
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public IntValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		var basicValue = basicInterpreter.newOperation(insn);
		if (basicValue == null) {
			return null;
		}

		if (InsnNodeUtils.isIntConstant(insn)) {
			return IntValue.newConstant(basicValue, InsnNodeUtils.getIntConstant(insn));
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public IntValue copyOperation(AbstractInsnNode insn, IntValue value) throws AnalyzerException {
		return value;
	}

	@Override
	public IntValue unaryOperation(AbstractInsnNode insn, IntValue value) throws AnalyzerException {
		var basicValue = basicInterpreter.unaryOperation(insn, value.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		if (value.isUnknown()) {
			return IntValue.newUnknown(basicValue);
		}

		var set = ImmutableSet.<Integer>builder();

		for (var v : value.getIntValues()) {
			switch (insn.getOpcode()) {
			case Opcodes.INEG:
				set.add(-v);
				break;
			case Opcodes.IINC:
				var iinc = (IincInsnNode) insn;
				set.add(v + iinc.incr);
				break;
			case Opcodes.I2B:
				set.add((int) (byte) (int) v);
				break;
			case Opcodes.I2C:
				set.add((int) (char) (int) v);
				break;
			case Opcodes.I2S:
				set.add((int) (short) (int) v);
				break;
			default:
				return IntValue.newUnknown(basicValue);
			}
		}

		return IntValue.newConstant(basicValue, set.build());
	}

	@Override
	public IntValue binaryOperation(AbstractInsnNode insn, IntValue value1, IntValue value2) throws AnalyzerException {
		var basicValue = basicInterpreter.binaryOperation(insn, value1.getBasicValue(), value2.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		if (value1.isUnknown() || value2.isUnknown()) {
			return IntValue.newUnknown(basicValue);
		}

		var set = ImmutableSet.<Integer>builder();

		for (var v1 : value1.getIntValues()) {
			for (var v2 : value2.getIntValues()) {
				switch (insn.getOpcode()) {
				case Opcodes.IADD:
					set.add(v1 + v2);
					break;
				case Opcodes.ISUB:
					set.add(v1 - v2);
					break;
				case Opcodes.IMUL:
					set.add(v1 * v2);
					break;
				case Opcodes.IDIV:
					if (v2 == 0) {
						return IntValue.newUnknown(basicValue);
					}
					set.add(v1 / v2);
					break;
				case Opcodes.IREM:
					if (v2 == 0) {
						return IntValue.newUnknown(basicValue);
					}
					set.add(v1 % v2);
					break;
				case Opcodes.ISHL:
					set.add(v1 << v2);
					break;
				case Opcodes.ISHR:
					set.add(v1 >> v2);
					break;
				case Opcodes.IUSHR:
					set.add(v1 >>> v2);
					break;
				case Opcodes.IAND:
					set.add(v1 & v2);
					break;
				case Opcodes.IOR:
					set.add(v1 | v2);
					break;
				case Opcodes.IXOR:
					set.add(v1 ^ v2);
					break;
				default:
					return IntValue.newUnknown(basicValue);
				}
			}
		}

		return IntValue.newConstant(basicValue, set.build());
	}

	@Override
	public IntValue ternaryOperation(AbstractInsnNode insn, IntValue value1, IntValue value2, IntValue value3) throws AnalyzerException {
		var basicValue = basicInterpreter.ternaryOperation(insn, value1.getBasicValue(), value2.getBasicValue(), value3.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public IntValue naryOperation(AbstractInsnNode insn, List<? extends IntValue> values) throws AnalyzerException {
		var args = values.stream()
			.map(IntValue::getBasicValue)
			.collect(ImmutableList.toImmutableList());
		var basicValue = basicInterpreter.naryOperation(insn, args);
		if (basicValue == null) {
			return null;
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, IntValue value, IntValue expected) throws AnalyzerException {
		basicInterpreter.returnOperation(insn, value.getBasicValue(), expected.getBasicValue());
	}

	@Override
	public IntValue merge(IntValue value1, IntValue value2) {
		var basicValue = basicInterpreter.merge(value1.getBasicValue(), value2.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		if (value1.isUnknown() || value2.isUnknown()) {
			return IntValue.newUnknown(basicValue);
		}

		var set = ImmutableSet.copyOf(Sets.union(value1.getIntValues(), value2.getIntValues()));
		if (set.size() > MAX_TRACKED_VALUES) {
			return IntValue.newUnknown(basicValue);
		}

		return IntValue.newConstant(basicValue, set);
	}
}
