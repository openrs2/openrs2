package org.openrs2.deob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD })
public @interface OriginalMember {
	String owner();
	String name();
	String descriptor();
}
