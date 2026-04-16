// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.lsp.protocol.tools.GetErrorsResult;

class GetErrorsToolTests {

	private final GetErrorsTool tool = new GetErrorsTool();

	@Test
	void testValidateInputWithValidInput() {
		Object input = new ArrayList<>(Arrays.asList("file1.java", "file2.java"));
		List<String> result = tool.validateInput(input);
		assertNotNull(result);
		assertEquals(input, result);
	}

	@Test
	void testValidateInputWithInvalidInputArray() {
		Object input = new ArrayList<>(Arrays.asList(123, "file2.java"));
		List<String> result = tool.validateInput(input);
		Assertions.assertNull(result);
	}

	@Test
	void testValidateInputWithValidInputString() {
		Object input = new String("[\"file1.java\", \"file2.java\"]");
		List<String> result = tool.validateInput(input);
		assertNotNull(result);
		assertEquals(Arrays.asList("file1.java", "file2.java"), result);
	}

	@Test
	void testValidateInputWithMalformedJsonEscapes() {
		// This simulates the case where the model sends a JSON string with paths
		// that have backslashes which become invalid JSON escape sequences after
		// the outer JSON parsing. For example, "[\"\TRL_EN\\.adt\\..."]" contains
		// invalid escapes like \T, \., \a, \c, \z which Gson cannot parse.
		// The string below represents what we receive after the outer JSON is parsed.
		Object input = "[\"\\TRL_EN\\.adt\\classlib\\classes\\z_class_eclipse\\z_class_eclipse.aclass\"]";
		List<String> result = tool.validateInput(input);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("\\TRL_EN\\.adt\\classlib\\classes\\z_class_eclipse\\z_class_eclipse.aclass", result.get(0));
	}

	@Test
	void testValidateInputWithMalformedJsonEscapesMultipleFiles() {
		Object input = "[\"C:\\Users\\file1.java\", \"C:\\Users\\file2.java\"]";
		List<String> result = tool.validateInput(input);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("C:\\Users\\file1.java", result.get(0));
		assertEquals("C:\\Users\\file2.java", result.get(1));
	}

	@Test
	void testValidateInputWithInvalidInputString() {
		Object input = new String("This is a input string");
		List<String> result = tool.validateInput(input);
		Assertions.assertNull(result);
	}

	@Test
	void testValidateInputWithNull() {
		List<String> result = tool.validateInput(null);
		Assertions.assertNull(result);
	}

	@Test
	void testGetErrorsWithInvalidFilePath() {
		// Prepare input with invalid file paths
		ArrayList<String> filePaths = new ArrayList<>(Arrays.asList("invalidPath1", "invalidPath2"));

		// Call the getErrors method
		GetErrorsResult result = tool.getErrors(filePaths);

		// Assert that the result contains the expected message
		assertTrue(result.content().contains("Resource not found for filePath: invalidPath1"));
		assertTrue(result.content().contains("Resource not found for filePath: invalidPath2"));
	}
}
