package dev.openrs2.asm;

import java.util.Objects;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class MemberDesc {
	private final String name, desc;

	public MemberDesc(FieldNode field) {
		this(field.name, field.desc);
	}

	public MemberDesc(MethodNode method) {
		this(method.name, method.desc);
	}

	public MemberDesc(FieldInsnNode fieldInsn) {
		this(fieldInsn.name, fieldInsn.desc);
	}

	public MemberDesc(MethodInsnNode methodInsn) {
		this(methodInsn.name, methodInsn.desc);
	}

	public MemberDesc(MemberRef memberRef) {
		this(memberRef.getName(), memberRef.getDesc());
	}

	public MemberDesc(String name, String desc) {
		this.name = name;
		this.desc = desc;
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
		var memberDesc = (MemberDesc) o;
		return name.equals(memberDesc.name) &&
			desc.equals(memberDesc.desc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, desc);
	}

	@Override
	public String toString() {
		return String.format("%s %s", desc, name);
	}
}
