package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

@ExtendWith(MockitoExtension.class)
class EditorManagerTests {

  @Mock
  private CopilotLanguageServerConnection mockServer;

  @Mock
  private CompletionProvider mockProvider;

  @Mock
  private IPreferenceStore mockPreferenceStore;

  @Test
  void testCreateHandlerForNull() {
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockPreferenceStore);

    assertNull(manager.getOrCreateCompletionHandlerFor(null));
  }

  @Test
  void testGetOrCreateCompletionHandlerForReturnsNewHandlerWhenNotPresent() {
    ITextEditor mockEditor = mock(ITextEditor.class);

    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockPreferenceStore);
    CompletionHandler handler = manager.getOrCreateCompletionHandlerFor(mockEditor);

    assertNotNull(handler);
  }

  @Test
  void testGetActiveHandlerWhenNoActiveEditor() {
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockPreferenceStore);

    assertNull(manager.getActiveCompletionHandler());
  }

  @Test
  void testGetActiveHandlerWhenActiveEditor() {
    ITextEditor mockEditor = mock(ITextEditor.class);
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockPreferenceStore);
    manager.getOrCreateCompletionHandlerFor(mockEditor);
    manager.setActiveEditor(mockEditor);

    assertNotNull(manager.getActiveCompletionHandler());
  }
}
