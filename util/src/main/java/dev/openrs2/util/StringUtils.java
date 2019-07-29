package dev.openrs2.util;

import com.google.common.base.Preconditions;

public final class StringUtils {
	public static String indefiniteArticle(String str) {
		Preconditions.checkArgument(!str.isEmpty());

		var first = Character.toLowerCase(str.charAt(0));
		if (first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u') {
			return "an";
		} else {
			return "a";
		}
	}

	public static String capitalize(String str) {
		if (str.isEmpty()) {
			return str;
		}

		var first = Character.toUpperCase(str.charAt(0));
		return first + str.substring(1);
	}

	private StringUtils() {
		/* empty */
	}
}
