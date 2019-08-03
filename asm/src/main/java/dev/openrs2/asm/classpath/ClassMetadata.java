package dev.openrs2.asm.classpath;

import java.util.List;
import java.util.Objects;

import dev.openrs2.asm.MemberDesc;

public abstract class ClassMetadata {
	public abstract String getName();
	public abstract boolean isDependency();
	public abstract boolean isInterface();
	public abstract ClassMetadata getSuperClass();
	public abstract List<ClassMetadata> getSuperInterfaces();
	public abstract List<MemberDesc> getFields();
	public abstract List<MemberDesc> getMethods();
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
