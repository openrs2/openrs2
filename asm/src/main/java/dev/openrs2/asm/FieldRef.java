package dev.openrs2.asm;

import java.util.Objects;

public final class FieldRef {
	private final String owner, name, desc;

	public FieldRef(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		FieldRef fieldRef = (FieldRef) o;
		return owner.equals(fieldRef.owner) &&
			name.equals(fieldRef.name) &&
			desc.equals(fieldRef.desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name, desc);
	}

	@Override
	public String toString() {
		return String.format("%s %s.%s", desc, owner, name);
	}
}
