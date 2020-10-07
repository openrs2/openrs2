package org.openrs2.deob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
public @interface OriginalClass {
	String value();
}
