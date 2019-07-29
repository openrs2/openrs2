package dev.openrs2.deob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface OriginalName {
	String value();
}
