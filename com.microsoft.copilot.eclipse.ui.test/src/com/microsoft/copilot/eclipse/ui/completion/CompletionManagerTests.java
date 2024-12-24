package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.completion.CompletionCollection;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class CompletionManagerTests {

  private IProject project;

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @BeforeEach
  public void setUp() throws Exception {
    project = ResourcesPlugin.getWorkspace().getRoot().getProject("TestProject");
    project.create(null);
    project.open(null);
  }

  @AfterEach
  public void tearDown() throws Exception {
    project.delete(true, null);
  }

  @Test
  void testReplaceCompletion_1() throws Exception {
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
    ITextViewer textViewer = (ITextViewer) textEditor.getAdapter(ITextOperationTarget.class);
    IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

    when(mockLsConnection.getDocumentVersion(any())).thenReturn(documentVersion);
    CompletionManager manager = new CompletionManager(mockLsConnection, mock(CompletionProvider.class), textViewer,
        document, file.getLocationURI());

    List<CompletionItem> completions = List.of(new CompletionItem("uuid", "    System.out.println(\"hi\");",
        new Range(new Position(3, 0), new Position(3, 27)), "hi\");", new Position(3, 24), documentVersion));
    CompletionCollection completionsCollection = new CompletionCollection(completions,
        file.getLocationURI().toASCIIString());

    manager.onCompletionResolved(completionsCollection);
    manager.acceptSuggestion();

    assertTrue(document.get().contains("  System.out.println(\"hi\");\n"));
  }

  protected IEditorPart getEditorPartFor(IFile file) {
    AtomicReference<IEditorPart> ref = new AtomicReference<>();
    SwtUtils.invokeOnDisplayThread(() -> {
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      if (window != null) {
        try {
          ref.set(window.getActivePage().openEditor(new org.eclipse.ui.part.FileEditorInput(file),
              "org.eclipse.ui.DefaultTextEditor"));
        } catch (PartInitException e) {
        }
      }
    });
    return ref.get();
  }

}
