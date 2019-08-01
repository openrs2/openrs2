package dev.openrs2.deob.classpath;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.openrs2.asm.MemberDesc;
import org.objectweb.asm.Type;

public final class ReflectionClassMetadata extends ClassMetadata {
	private final ClassPath classPath;
	private final Class<?> clazz;

	public ReflectionClassMetadata(ClassPath classPath, Class<?> clazz) {
		this.classPath = classPath;
		this.clazz = clazz;
	}

	@Override
	public String getName() {
		return clazz.getName().replace('.', '/');
	}

	@Override
	public boolean isDependency() {
		return true;
	}

	@Override
	public boolean isInterface() {
		return clazz.isInterface();
	}

	@Override
	public ClassMetadata getSuperClass() {
		var superClass = clazz.getSuperclass();
		if (superClass != null) {
			return classPath.get(superClass.getName().replace('.', '/'));
		}
		return null;
	}

	@Override
	public List<ClassMetadata> getSuperInterfaces() {
		return Arrays.stream(clazz.getInterfaces())
			.map(i -> classPath.get(i.getName().replace('.', '/')))
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public List<MemberDesc> getFields() {
		return Arrays.stream(clazz.getDeclaredFields())
			.map(f -> new MemberDesc(f.getName(), Type.getDescriptor(f.getType())))
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public List<MemberDesc> getMethods() {
		return Arrays.stream(clazz.getDeclaredMethods())
			.map(m -> new MemberDesc(m.getName(), Type.getMethodDescriptor(m)))
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public boolean isNative(MemberDesc method) {
		for (var m : clazz.getDeclaredMethods()) {
			if (m.getName().equals(method.getName()) && Type.getMethodDescriptor(m).equals(method.getDesc())) {
				return Modifier.isNative(m.getModifiers());
			}
		}
		return false;
	}
}
