package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.List;

import dev.openrs2.asm.Transformer;
import dev.openrs2.deob.annotation.OriginalClass;
import dev.openrs2.deob.annotation.OriginalMember;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class OriginalNameTransformer extends Transformer {
	private static AnnotationNode createOriginalClassAnnotation(String name) {
		var annotation = new AnnotationNode(Type.getDescriptor(OriginalClass.class));
		annotation.values = List.of("value", name);
		return annotation;
	}

	private static AnnotationNode createOriginalMemberAnnotation(String owner, String name, String desc) {
		var annotation = new AnnotationNode(Type.getDescriptor(OriginalMember.class));
		annotation.values = List.of(
			"owner", owner,
			"name", name,
			"descriptor", desc
		);
		return annotation;
	}

	private final Remapper remapper;

	public OriginalNameTransformer(Remapper remapper) {
		this.remapper = remapper;
	}

	@Override
	public void transformClass(ClassNode clazz) {
		if (clazz.name.equals(remapper.map(clazz.name))) {
			return;
		}

		if (clazz.invisibleAnnotations == null) {
			clazz.invisibleAnnotations = new ArrayList<>();
		}
		clazz.invisibleAnnotations.add(createOriginalClassAnnotation(clazz.name));
	}

	@Override
	public void transformField(ClassNode clazz, FieldNode field) {
		if (field.name.equals(remapper.mapFieldName(clazz.name, field.name, field.desc))) {
			return;
		}

		if (field.invisibleAnnotations == null) {
			field.invisibleAnnotations = new ArrayList<>();
		}
		field.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, field.name, field.desc));
	}

	@Override
	public void transformMethod(ClassNode clazz, MethodNode method) {
		if (method.name.equals(remapper.mapMethodName(clazz.name, method.name, method.desc))) {
			return;
		}

		if (method.invisibleAnnotations == null) {
			method.invisibleAnnotations = new ArrayList<>();
		}
		method.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, method.name, method.desc));
	}
}
