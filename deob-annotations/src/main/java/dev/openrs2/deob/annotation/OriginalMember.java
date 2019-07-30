package dev.openrs2.deob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface OriginalMember {
	String owner();
	String name();
	String descriptor();
}
