package dev.openrs2.deob.analysis;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public final class ConstSourceValue implements Value {
	public static ConstSourceValue createUnknown(BasicValue basicValue) {
		Preconditions.checkNotNull(basicValue);
		return new ConstSourceValue(basicValue, null);
	}

	public static ConstSourceValue createSingleSourceConstant(BasicValue basicValue, AbstractInsnNode source) {
		Preconditions.checkArgument(basicValue == BasicValue.INT_VALUE);
		Preconditions.checkNotNull(source);
		return new ConstSourceValue(basicValue, source);
	}

	private final BasicValue basicValue;
	private final AbstractInsnNode source;

	private ConstSourceValue(BasicValue basicValue, AbstractInsnNode source) {
		this.basicValue = basicValue;
		this.source = source;
	}

	public BasicValue getBasicValue() {
		return basicValue;
	}

	public boolean isUnknown() {
		return source == null;
	}

	public boolean isSingleSourceConstant() {
		return source != null;
	}

	public AbstractInsnNode getSource() {
		Preconditions.checkState(source != null);
		return source;
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
		ConstSourceValue that = (ConstSourceValue) o;
		return basicValue.equals(that.basicValue) &&
			Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(basicValue, source);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("basicValue", basicValue)
			.add("source", source)
			.toString();
	}
}
