package dev.openrs2.deob.analysis;

import java.util.Objects;

import com.google.common.base.Preconditions;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public final class IntValue implements Value {
	public static IntValue newConstant(BasicValue basicValue, int intValue) {
		Preconditions.checkArgument(basicValue == BasicValue.INT_VALUE);
		return new IntValue(basicValue, intValue);
	}

	public static IntValue newUnknown(BasicValue basicValue) {
		Preconditions.checkNotNull(basicValue);
		return new IntValue(basicValue, null);
	}

	private final BasicValue basicValue;
	private final Integer intValue;

	private IntValue(BasicValue basicValue, Integer intValue) {
		this.basicValue = basicValue;
		this.intValue = intValue;
	}

	public BasicValue getBasicValue() {
		return basicValue;
	}

	public boolean isConstant() {
		return intValue != null;
	}

	public int getIntValue() {
		Preconditions.checkState(intValue != null);
		return intValue;
	}

	@Override
	public int getSize() {
		return basicValue.getSize();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IntValue intValue1 = (IntValue) o;
		return basicValue.equals(intValue1.basicValue) &&
			Objects.equals(intValue, intValue1.intValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(basicValue, intValue);
	}
}
