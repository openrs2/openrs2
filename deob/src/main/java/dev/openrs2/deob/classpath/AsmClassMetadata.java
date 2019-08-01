package dev.openrs2.deob.classpath;

import java.util.List;
import java.util.stream.Collectors;

import dev.openrs2.asm.MemberDesc;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public final class AsmClassMetadata extends ClassMetadata {
	private final ClassPath classPath;
	private final ClassNode clazz;
	private final boolean dependency;

	public AsmClassMetadata(ClassPath classPath, ClassNode clazz, boolean dependency) {
		this.classPath = classPath;
		this.clazz = clazz;
		this.dependency = dependency;
	}

	@Override
	public String getName() {
		return clazz.name;
	}

	@Override
	public boolean isDependency() {
		return dependency;
	}

	@Override
	public boolean isInterface() {
		return (clazz.access & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public ClassMetadata getSuperClass() {
		if (clazz.superName != null) {
			return classPath.get(clazz.superName);
		}
		return null;
	}

	@Override
	public List<ClassMetadata> getSuperInterfaces() {
		return clazz.interfaces.stream()
			.map(classPath::get)
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public List<MemberDesc> getFields() {
		return clazz.fields.stream()
			.map(f -> new MemberDesc(f.name, f.desc))
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public List<MemberDesc> getMethods() {
		return clazz.methods.stream()
			.map(m -> new MemberDesc(m.name, m.desc))
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public boolean isNative(MemberDesc method) {
		for (var m : clazz.methods) {
			if (m.name.equals(method.getName()) && m.desc.equals(method.getDesc())) {
				return (m.access & Opcodes.ACC_NATIVE) != 0;
			}
		}
		return false;
	}
}
