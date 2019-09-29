package dev.openrs2.deob.analysis;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.util.collect.DisjointSet;

public final class SourcedIntValue {
	private final DisjointSet.Partition<MemberRef> source;
	private final IntValue intValue;

	public SourcedIntValue(DisjointSet.Partition<MemberRef> source, IntValue intValue) {
		this.source = source;
		this.intValue = intValue;
	}

	public DisjointSet.Partition<MemberRef> getSource() {
		return source;
	}

	public IntValue getIntValue() {
		return intValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SourcedIntValue that = (SourcedIntValue) o;
		return source.equals(that.source) &&
			intValue.equals(that.intValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, intValue);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("source", source)
			.add("intValue", intValue)
			.toString();
	}
}
