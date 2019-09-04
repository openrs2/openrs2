package dev.openrs2.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class StringUtilsTest {
	@Test
	public void testIndefiniteArticle() {
		assertEquals("a", StringUtils.indefiniteArticle("book"));
		assertEquals("an", StringUtils.indefiniteArticle("aeroplane"));

		assertThrows(IllegalArgumentException.class, () -> StringUtils.indefiniteArticle(""));
	}

	@Test
	public void testCapitalize() {
		assertEquals("Hello", StringUtils.capitalize("hello"));
		assertEquals("", StringUtils.capitalize(""));
	}
}
