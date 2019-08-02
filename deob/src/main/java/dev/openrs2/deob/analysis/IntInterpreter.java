package dev.openrs2.deob.analysis;

import java.util.List;
import java.util.stream.Collectors;

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
	private final Interpreter<BasicValue> basicInterpreter = new BasicInterpreter();

	public IntInterpreter() {
		super(Opcodes.ASM7);
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

		if (value.isConstant()) {
			var v = value.getIntValue();

			switch (insn.getOpcode()) {
			case Opcodes.INEG:
				return IntValue.newConstant(basicValue, -v);
			case Opcodes.IINC:
				var iinc = (IincInsnNode) insn;
				return IntValue.newConstant(basicValue, v + iinc.incr);
			case Opcodes.I2B:
				return IntValue.newConstant(basicValue, (byte) v);
			case Opcodes.I2C:
				return IntValue.newConstant(basicValue, (char) v);
			case Opcodes.I2S:
				return IntValue.newConstant(basicValue, (short) v);
			}
		}

		return IntValue.newUnknown(basicValue);
	}

	@Override
	public IntValue binaryOperation(AbstractInsnNode insn, IntValue value1, IntValue value2) throws AnalyzerException {
		var basicValue = basicInterpreter.binaryOperation(insn, value1.getBasicValue(), value2.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		if (value1.isConstant() && value2.isConstant()) {
			var v1 = value1.getIntValue();
			var v2 = value2.getIntValue();

			switch (insn.getOpcode()) {
			case Opcodes.IADD:
				return IntValue.newConstant(basicValue, v1 + v2);
			case Opcodes.ISUB:
				return IntValue.newConstant(basicValue, v1 - v2);
			case Opcodes.IMUL:
				return IntValue.newConstant(basicValue, v1 * v2);
			case Opcodes.IDIV:
				if (v2 == 0) {
					return IntValue.newUnknown(basicValue);
				}
				return IntValue.newConstant(basicValue, v1 / v2);
			case Opcodes.IREM:
				if (v2 == 0) {
					return IntValue.newUnknown(basicValue);
				}
				return IntValue.newConstant(basicValue, v1 % v2);
			case Opcodes.ISHL:
				return IntValue.newConstant(basicValue, v1 << v2);
			case Opcodes.ISHR:
				return IntValue.newConstant(basicValue, v1 >> v2);
			case Opcodes.IUSHR:
				return IntValue.newConstant(basicValue, v1 >>> v2);
			case Opcodes.IAND:
				return IntValue.newConstant(basicValue, v1 & v2);
			case Opcodes.IOR:
				return IntValue.newConstant(basicValue, v1 | v2);
			case Opcodes.IXOR:
				return IntValue.newConstant(basicValue, v1 ^ v2);
			}
		}

		return IntValue.newUnknown(basicValue);
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
			.collect(Collectors.toUnmodifiableList());
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

		if (value1.isConstant() && value2.isConstant() && value1.getIntValue() == value2.getIntValue()) {
			return IntValue.newConstant(basicValue, value1.getIntValue());
		}

		return IntValue.newUnknown(basicValue);
	}
}
