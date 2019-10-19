package dev.openrs2.asm;

import java.util.Objects;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class MemberRef {
	private final String owner, name, desc;

	public MemberRef(ClassNode clazz, FieldNode field) {
		this(clazz.name, field.name, field.desc);
	}

	public MemberRef(ClassNode clazz, MethodNode method) {
		this(clazz.name, method.name, method.desc);
	}

	public MemberRef(FieldInsnNode fieldInsn) {
		this(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
	}

	public MemberRef(MethodInsnNode methodInsn) {
		this(methodInsn.owner, methodInsn.name, methodInsn.desc);
	}

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
		var memberRef = (MemberRef) o;
		return owner.equals(memberRef.owner) &&
			name.equals(memberRef.name) &&
			desc.equals(memberRef.desc);
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
