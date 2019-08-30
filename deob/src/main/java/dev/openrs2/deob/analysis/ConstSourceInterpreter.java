package dev.openrs2.deob.analysis;

import java.util.List;

import com.google.common.collect.ImmutableList;
import dev.openrs2.asm.InsnNodeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Interpreter;

public final class ConstSourceInterpreter extends Interpreter<ConstSourceValue> {
	private final Interpreter<BasicValue> basicInterpreter = new BasicInterpreter();

	public ConstSourceInterpreter() {
		super(Opcodes.ASM7);
	}

	@Override
	public ConstSourceValue newValue(Type type) {
		var basicValue = basicInterpreter.newValue(type);
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		var basicValue = basicInterpreter.newOperation(insn);
		if (basicValue == null) {
			return null;
		}

		if (InsnNodeUtils.isIntConstant(insn)) {
			return ConstSourceValue.createSingleSourceConstant(basicValue, insn);
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue copyOperation(AbstractInsnNode insn, ConstSourceValue value) throws AnalyzerException {
		var basicValue = basicInterpreter.copyOperation(insn, value.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue unaryOperation(AbstractInsnNode insn, ConstSourceValue value) throws AnalyzerException {
		var basicValue = basicInterpreter.unaryOperation(insn, value.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue binaryOperation(AbstractInsnNode insn, ConstSourceValue value1, ConstSourceValue value2) throws AnalyzerException {
		var basicValue = basicInterpreter.binaryOperation(insn, value1.getBasicValue(), value2.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue ternaryOperation(AbstractInsnNode insn, ConstSourceValue value1, ConstSourceValue value2, ConstSourceValue value3) throws AnalyzerException {
		var basicValue = basicInterpreter.ternaryOperation(insn, value1.getBasicValue(), value2.getBasicValue(), value3.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public ConstSourceValue naryOperation(AbstractInsnNode insn, List<? extends ConstSourceValue> values) throws AnalyzerException {
		var args = values.stream()
			.map(ConstSourceValue::getBasicValue)
			.collect(ImmutableList.toImmutableList());
		var basicValue = basicInterpreter.naryOperation(insn, args);
		if (basicValue == null) {
			return null;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, ConstSourceValue value, ConstSourceValue expected) throws AnalyzerException {
		basicInterpreter.returnOperation(insn, value.getBasicValue(), expected.getBasicValue());
	}

	@Override
	public ConstSourceValue merge(ConstSourceValue value1, ConstSourceValue value2) {
		var basicValue = basicInterpreter.merge(value1.getBasicValue(), value2.getBasicValue());
		if (basicValue == null) {
			return null;
		}

		if (value1.isSingleSourceConstant() && value1.equals(value2)) {
			return value1;
		}

		return ConstSourceValue.createUnknown(basicValue);
	}
}
