package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.completion.AcceptSuggestionType;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

@ExtendWith(MockitoExtension.class)
class CompletionManagerTests extends CompletionBaseTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Test
  void testReplaceCompletion1() throws Exception {
    IFile file = project.getFile("Test.java");
    String content = """
          public class App {

          public void hi() {
            System.out.println("");
          }

        }
        """;
    file.create(content.getBytes(), IResource.FORCE, null);
    int documentVersion = 1;

    IEditorPart editorPart = getEditorPartFor(file);
    assertTrue(editorPart instanceof ITextEditor);

    ITextEditor textEditor = (ITextEditor) editorPart;

    when(mockLsConnection.getDocumentVersion(any())).thenReturn(documentVersion);
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    LanguageServerSettingManager languageServerSettingManager = mock(LanguageServerSettingManager.class);
    when(languageServerSettingManager.getSettings()).thenReturn(settings);
    CompletionManager manager = new CompletionManager(mockLsConnection, mock(CompletionProvider.class), textEditor,
        languageServerSettingManager);

    List<CompletionItem> completions = List.of(new CompletionItem("uuid", "    System.out.println(\"hi\");",
        new Range(new Position(3, 0), new Position(3, 27)), "hi\");", new Position(3, 24), documentVersion));

    manager.onCompletionResolved(LSPEclipseUtils.toUri(file.getLocation().toFile()).toASCIIString(), completions);
    manager.acceptSuggestion(AcceptSuggestionType.FULL);

    IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
    assertTrue(document.get().contains("  System.out.println(\"hi\");\n"));
  }

}
