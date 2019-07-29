package dev.openrs2.asm;

import java.util.Objects;

public final class MemberRef {
	private final String owner, name, desc;

	public MemberRef(String owner, MemberDesc desc) {
		this(owner, desc.getName(), desc.getDesc());
	}

	public MemberRef(String owner, String name, String desc) {
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
		MemberRef fieldRef = (MemberRef) o;
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
