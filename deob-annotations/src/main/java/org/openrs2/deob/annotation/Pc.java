package org.openrs2.deob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.LOCAL_VARIABLE, ElementType.PARAMETER })
public @interface Pc {
	int value();
}
