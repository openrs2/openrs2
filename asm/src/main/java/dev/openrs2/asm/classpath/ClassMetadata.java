package dev.openrs2.asm.classpath;

import java.util.Objects;

import com.google.common.collect.ImmutableList;
import dev.openrs2.asm.MemberDesc;

public abstract class ClassMetadata {
	public abstract String getName();
	public abstract boolean isDependency();
	public abstract boolean isInterface();
	public abstract ClassMetadata getSuperClass();
	public abstract ImmutableList<ClassMetadata> getSuperInterfaces();
	public abstract ImmutableList<MemberDesc> getFields();
	public abstract ImmutableList<MemberDesc> getMethods();
	public abstract boolean isNative(MemberDesc method);

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		var that = (ClassMetadata) o;
		return getName().equals(that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName());
	}
}
