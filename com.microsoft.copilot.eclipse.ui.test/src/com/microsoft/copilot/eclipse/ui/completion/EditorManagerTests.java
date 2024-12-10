package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;

@ExtendWith(MockitoExtension.class)
class EditorManagerTests {

  @Test
  void testCreateHandlerForNull() {
    CopilotLanguageServerConnection mockServer = mock(CopilotLanguageServerConnection.class);
    EditorsManager manager = new EditorsManager(mockServer);

    assertNull(manager.getOrCreateCompletionHandlerFor(null));
  }

  @Test
  void getOrCreateCompletionHandlerForReturnsNewHandlerWhenNotPresent() {
    ITextEditor mockEditor = mock(ITextEditor.class);
    CopilotLanguageServerConnection mockServer = mock(CopilotLanguageServerConnection.class);

    EditorsManager manager = new EditorsManager(mockServer);
    CompletionHandler handler = manager.getOrCreateCompletionHandlerFor(mockEditor);

    assertNotNull(handler);
  }
}
