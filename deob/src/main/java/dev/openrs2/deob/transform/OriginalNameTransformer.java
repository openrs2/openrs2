package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.deob.annotation.OriginalArg;
import dev.openrs2.deob.annotation.OriginalClass;
import dev.openrs2.deob.annotation.OriginalMember;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class OriginalNameTransformer extends Transformer {
	private static AnnotationNode createOriginalClassAnnotation(String name) {
		var annotation = new AnnotationNode(Type.getDescriptor(OriginalClass.class));
		annotation.values = ImmutableList.of("value", name);
		return annotation;
	}

	private static AnnotationNode createOriginalMemberAnnotation(String owner, String name, String desc) {
		var annotation = new AnnotationNode(Type.getDescriptor(OriginalMember.class));
		annotation.values = ImmutableList.of(
			"owner", owner,
			"name", name,
			"descriptor", desc
		);
		return annotation;
	}

	private static AnnotationNode createOriginalArgAnnotation(int index) {
		var annotation = new AnnotationNode(Type.getDescriptor(OriginalArg.class));
		annotation.values = ImmutableList.of("value", index);
		return annotation;
	}

	@Override
	public boolean transformClass(ClassPath classPath, Library library, ClassNode clazz) {
		if (clazz.invisibleAnnotations == null) {
			clazz.invisibleAnnotations = new ArrayList<>();
		}
		clazz.invisibleAnnotations.add(createOriginalClassAnnotation(clazz.name));

		return false;
	}

	@Override
	public boolean transformField(ClassPath classPath, Library library, ClassNode clazz, FieldNode field) {
		if (field.invisibleAnnotations == null) {
			field.invisibleAnnotations = new ArrayList<>();
		}
		field.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, field.name, field.desc));

		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean preTransformMethod(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
			return false;
		}

		if (method.invisibleAnnotations == null) {
			method.invisibleAnnotations = new ArrayList<>();
		}
		method.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, method.name, method.desc));

		int args = Type.getArgumentTypes(method.desc).length;
		if (method.invisibleParameterAnnotations == null) {
			method.invisibleParameterAnnotations = (List<AnnotationNode>[]) new List<?>[args];
		}
		for (var i = 0; i < method.invisibleParameterAnnotations.length; i++) {
			var annotations = method.invisibleParameterAnnotations[i];
			if (annotations == null) {
				annotations = method.invisibleParameterAnnotations[i] = new ArrayList<>();
			}
			annotations.add(createOriginalArgAnnotation(i));
		}

		return false;
	}
}
