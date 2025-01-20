package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.core.format.JavaFormatReader;

@ExtendWith(MockitoExtension.class)
class FormatOptionProviderTests {
	private FormatOptionProvider formatOptionProvider;
	private IFile mockFile;
	private IProject mockProject;

	private static final String JavaCore_PLUGIN_ID = "org.eclipse.jdt.core";
	private static final int PREFERENCE_DEFAULT_TAB_SIZE = 2;

	@BeforeEach
	void setUp() {
		formatOptionProvider = new FormatOptionProvider();
		mockFile = mock(IFile.class);
		mockProject = mock(IProject.class);

		when(mockFile.exists()).thenReturn(true);
		when(mockFile.isAccessible()).thenReturn(true);
		when(mockFile.getProject()).thenReturn(mockProject);
	}

	@Test
	void testGetEclipseDefaultJavaTabCharAndSize() {
		when(mockProject.getName()).thenReturn("testProject");
		when(mockFile.getFileExtension()).thenReturn("java");

		JavaFormatReader javaFormatReader = new JavaFormatReader(mockProject);
		boolean useSpace = javaFormatReader.getUseSpaces();
		int tabSize = javaFormatReader.getIndentSize();

		assertEquals(useSpace, formatOptionProvider.useSpace(mockFile));
		assertEquals(tabSize, formatOptionProvider.getTabSize(mockFile));
	}

	@Test
	void testGetCopilotDefaultTabCharAndSizeForUnknownLanguage() {
		when(mockFile.getFileExtension()).thenReturn("js");

		assertTrue(formatOptionProvider.useSpace(mockFile));
		assertEquals(PREFERENCE_DEFAULT_TAB_SIZE, formatOptionProvider.getTabSize(mockFile));
	}

	@Test
	void testGetCopilotDefaultTabCharAndSizeForNoExtensionFile() {
		when(mockFile.getFileExtension()).thenReturn(null);

		assertTrue(formatOptionProvider.useSpace(mockFile));
		assertEquals(PREFERENCE_DEFAULT_TAB_SIZE, formatOptionProvider.getTabSize(mockFile));
	}

}
