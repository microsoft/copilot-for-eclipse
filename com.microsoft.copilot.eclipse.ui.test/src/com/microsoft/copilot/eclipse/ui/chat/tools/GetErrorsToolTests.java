package com.microsoft.copilot.eclipse.ui.chat.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

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
	void testResolveFilePathWithAbsolutePath() {
		if (PlatformUtils.isWindows()) {
			// Test with a Windows-like absolute path
			String windowsPath = "C:\\Users\\User\\project\\file.java";
			URI resultWindows = tool.resolveFilePath(windowsPath);
			assertNotNull(resultWindows);
			assertEquals("file:///C:/Users/User/project/file.java", resultWindows.toString());
		} else {
			// Test with a POSIX-like absolute path
			String posixPath = "/home/user/project/file.java";
			URI result = tool.resolveFilePath(posixPath);
			assertNotNull(result);
			assertEquals("file:///home/user/project/file.java", result.toString());
		}
	}

	@Test
	void testResolveFilePathWithInvalidPath() {
		// Test with an invalid URI
		String invalidPath = ":://///////invalidPath";
		URI result = tool.resolveFilePath(invalidPath);
		Assertions.assertNull(result);
	}

	@Test
	void testResolveFilePathWithRelativePath() {
		// Test with a relative path
		String relativePath = "src/com/example/Main.java";
		URI result = tool.resolveFilePath(relativePath);
		Assertions.assertNull(result); // Relative paths are not resolved to URIs
	}

	@Test
	void testGetErrorsWithNullResolvedFilePath() {
		// Create a subclass to override the resolveFilePath method
		GetErrorsTool tool = new GetErrorsTool() {
			@Override
			public URI resolveFilePath(String filepath) {
				return null; // Simulate resolveFilePath returning null
			}
		};

		// Prepare input file URIs
		ArrayList<String> fileUris = new ArrayList<>(Arrays.asList("invalidPath1", "invalidPath2"));

		// Call the getErrors method
		String result = tool.getErrors(fileUris);

		// Assert that the result contains the expected message
		assertTrue(result.contains("Resource not found for fileUri: null"));
	}
}
