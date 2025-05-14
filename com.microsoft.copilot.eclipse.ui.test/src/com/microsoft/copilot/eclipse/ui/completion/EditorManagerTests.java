package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

@ExtendWith(MockitoExtension.class)
class EditorManagerTests {

  @Mock
  private CopilotLanguageServerConnection mockServer;

  @Mock
  private CompletionProvider mockProvider;

  @Mock
  private LanguageServerSettingManager mockSettingManager;

  @Test
  void testCreateManagerForNull() {
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockSettingManager);

    assertNull(manager.getOrCreateCompletionManagerFor(null));
  }

  @Test
  void testGetOrCreateCompletionManagerWhenNotPresent() {
    IEditorPart mockEditorPart = mock(IEditorPart.class);
    ITextEditor mockEditor = mock(ITextEditor.class);
    ITextViewer mockViewer = mock(ITextViewer.class);

    when(mockEditorPart.getAdapter(ITextEditor.class)).thenReturn(mockEditor);
    when(mockEditor.getAdapter(any())).thenReturn(mockViewer);
    when(mockViewer.isEditable()).thenReturn(true);

    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockSettingManager);
    CompletionManager completionManager = manager.getOrCreateCompletionManagerFor(mockEditorPart);

    assertNotNull(completionManager);
  }

  @Test
  void testGetActiveManagerWhenNoActiveEditor() {
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockSettingManager);

    assertNull(manager.getActiveCompletionManager());
  }

  @Test
  void testGetActiveHandlerWhenActiveEditor() {
    IEditorPart mockEditorPart = mock(IEditorPart.class);
    ITextEditor mockEditor = mock(ITextEditor.class);
    ITextViewer mockViewer = mock(ITextViewer.class);

    when(mockEditorPart.getAdapter(ITextEditor.class)).thenReturn(mockEditor);
    when(mockEditor.getAdapter(any())).thenReturn(mockViewer);
    when(mockViewer.isEditable()).thenReturn(true);
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockSettingManager);
    manager.getOrCreateCompletionManagerFor(mockEditorPart);
    manager.setActiveEditor(mockEditorPart);

    assertNotNull(manager.getActiveCompletionManager());
  }
}
