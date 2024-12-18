package com.microsoft.copilot.eclipse.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


class PlatformUtilsTests {

	@Test
	void testEscapeSpaceInUrl() {
		assertEquals("https://example.com/path%20with%20spaces",
				PlatformUtils.escapeSpaceInUrl("https://example.com/path with spaces"));
		assertEquals("https://example.com/nospaces", PlatformUtils.escapeSpaceInUrl("https://example.com/nospaces"));
		assertEquals("%20leading%20and%20trailing%20", PlatformUtils.escapeSpaceInUrl(" leading and trailing "));
		assertEquals("", PlatformUtils.escapeSpaceInUrl(""));
		assertEquals("%20", PlatformUtils.escapeSpaceInUrl(" "));
	}
}
