// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.lsp4j.FormattingOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.core.format.JavaFormatReader;

@ExtendWith(MockitoExtension.class)
class FormatOptionProviderTests {
  private FormatOptionProvider formatOptionProvider;
  private IFile mockFile;
  private IProject mockProject;

  private static final String EDITOR_PREF_NODE = "org.eclipse.ui.editors";
  private static final String TAB_WIDTH_KEY = "tabWidth";
  private static final String SPACES_FOR_TABS_KEY = "spacesForTabs";

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
    FormattingOptions languageFormat = javaFormatReader.getFormattingOptions();
    boolean useSpace = languageFormat.isInsertSpaces();
    int tabSize = languageFormat.getTabSize();

    assertEquals(useSpace, formatOptionProvider.useSpace(mockFile));
    assertEquals(tabSize, formatOptionProvider.getTabSize(mockFile));
  }

  @ParameterizedTest @NullSource @ValueSource(strings = { "js" })
  void testUsesEclipseTextEditorFormattingOptionsForUnknownOrNoExtension(String extension) {
    when(mockFile.getFileExtension()).thenReturn(extension);

    // Set the Eclipse preferences to be something other than the default (false, 4)
    setEditorFormattingPreferences(true, 2);

    assertTrue(formatOptionProvider.useSpace(mockFile));
    assertEquals(2, formatOptionProvider.getTabSize(mockFile));
  }

  private void setEditorFormattingPreferences(boolean useSpaces, int tabSize) {
    IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(EDITOR_PREF_NODE);
    prefs.putBoolean(SPACES_FOR_TABS_KEY, useSpaces);
    prefs.putInt(TAB_WIDTH_KEY, tabSize);
  }
}
