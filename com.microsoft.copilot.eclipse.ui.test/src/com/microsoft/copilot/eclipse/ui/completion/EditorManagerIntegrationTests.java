package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

class EditorManagerIntegrationTests extends CompletionBaseTests {

  @Mock
  private CopilotLanguageServerConnection mockServer;

  @Mock
  private CompletionProvider mockProvider;

  @Mock
  private LanguageServerSettingManager mockSettingManager;

  @Override
  @BeforeEach
  void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);

    doNothing().when(mockServer).connectDocument(any());
    doNothing().when(mockServer).disconnectDocument(any());

    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    when(mockSettingManager.getSettings()).thenReturn(settings);
  }

  @Test
  void testDocumentIsConnectedAndDisconnected() throws Exception {
    IFile file = project.getFile("Test.java");
    String content = """
        public class App {
        }
        """;
    file.create(content.getBytes(), IResource.FORCE, null);

    IEditorPart editorPart = getEditorPartFor(file);
    assertTrue(editorPart instanceof ITextEditor);

    ITextEditor textEditor = (ITextEditor) editorPart;
    EditorsManager manager = new EditorsManager(mockServer, mockProvider, mockSettingManager);
    manager.getOrCreateCompletionManagerFor(textEditor);
    // get completion manager for the same editor should not reconnect the document.
    manager.getOrCreateCompletionManagerFor(textEditor);
    manager.disposeCompletionManagerFor(textEditor);

    verify(mockServer, times(1)).connectDocument(any());
    verify(mockServer, times(1)).disconnectDocument(any());
  }
}
