package dev.openrs2.deob.analysis;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public final class IntValue implements Value {
	public static IntValue newConstant(BasicValue basicValue, int intValue) {
		return newConstant(basicValue, ImmutableSet.of(intValue));
	}

	public static IntValue newConstant(BasicValue basicValue, ImmutableSet<Integer> intValues) {
		Preconditions.checkArgument(basicValue == BasicValue.INT_VALUE);
		Preconditions.checkArgument(!intValues.isEmpty());
		return new IntValue(basicValue, intValues);
	}

	public static IntValue newUnknown(BasicValue basicValue) {
		Preconditions.checkNotNull(basicValue);
		return new IntValue(basicValue, ImmutableSet.of());
	}

	private final BasicValue basicValue;
	private final ImmutableSet<Integer> intValues;

	private IntValue(BasicValue basicValue, ImmutableSet<Integer> intValues) {
		this.basicValue = basicValue;
		this.intValues = intValues;
	}

	public BasicValue getBasicValue() {
		return basicValue;
	}

	public boolean isUnknown() {
		return intValues.isEmpty();
	}

	public boolean isSingleConstant() {
		return intValues.size() == 1;
	}

	public int getIntValue() {
		Preconditions.checkState(isSingleConstant());
		return intValues.iterator().next();
	}

	public ImmutableSet<Integer> getIntValues() {
		Preconditions.checkArgument(!isUnknown());
		return intValues;
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
			Objects.equals(intValues, intValue1.intValues);
	}

	@Override
	public int hashCode() {
		return Objects.hash(basicValue, intValues);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("basicValue", basicValue)
			.add("intValues", intValues)
			.toString();
	}
}
