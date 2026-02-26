package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotLanguageServerSettings;
import com.microsoft.copilot.eclipse.ui.preferences.LanguageServerSettingManager;

@ExtendWith(MockitoExtension.class)
class BaseCompletionManagerTests extends CompletionBaseTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private CompletionProvider mockCompletionProvider;

  @Mock
  private LanguageServerSettingManager mockSettingsManager;

  private TestableBaseCompletionManager completionManager;
  private IFile testFile;
  private ITextEditor textEditor;
  private URI documentUri;

  @BeforeEach
  void setUpTest() throws Exception {
    // Create test file
    testFile = project.getFile("TestFile.java");
    String content = """
        public class TestFile {
            public void testMethod() {
                System.out.println("test");
            }
        }
        """;
    testFile.create(content.getBytes(), IResource.FORCE, null);

    // Get editor and document URI
    IEditorPart editorPart = getEditorPartFor(testFile);
    textEditor = (ITextEditor) editorPart;
    documentUri = LSPEclipseUtils.toUri(testFile.getLocation().toFile());

    // Setup mocks
    CopilotLanguageServerSettings settings = new CopilotLanguageServerSettings();
    settings.setEnableAutoCompletions(true);
    when(mockSettingsManager.getSettings()).thenReturn(settings);

    // Create testable completion manager
    completionManager = new TestableBaseCompletionManager(mockLsConnection, mockCompletionProvider, textEditor,
        mockSettingsManager);
  }

  @Test
  void testCaretPositionChangeAndUpdatesDocumentVersion() throws Exception {
    // Initial setup - simulate document version changes
    when(mockLsConnection.getDocumentVersion(documentUri)).thenReturn(1, 2);

    // First handleCaretPositionChange call - should initialize document version but
    // not trigger completion
    MouseEvent mouseEvent = createMockMouseEvent();
    completionManager.mouseDown(mouseEvent);

    // Verify no completion triggered on first mouse down (only initializes version)
    verify(mockCompletionProvider, never()).triggerCompletion(any(), any(), any(Integer.class), any(Boolean.class));
    assertEquals(1, completionManager.getDocumentVersion());

    // Second handleCaretPositionChange call with updated document version - should
    // trigger completion
    completionManager.setModelOffset(10); // Set a model offset for testing
    completionManager.mouseDown(mouseEvent);

    // Verify completion was triggered with correct document version
    ArgumentCaptor<Integer> versionCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(mockCompletionProvider, times(1)).triggerCompletion(any(), any(Position.class), versionCaptor.capture(),any(Boolean.class));

    assertEquals(2, versionCaptor.getValue().intValue());
    assertEquals(2, completionManager.getDocumentVersion());
  }

  @Test
  void testSameDocumentVersionDoesNotTriggerCompletion() {
    // Setup same document version
    when(mockLsConnection.getDocumentVersion(documentUri)).thenReturn(1);

    // First handleCaretPositionChange call - initializes version
    MouseEvent mouseEvent = createMockMouseEvent();
    completionManager.mouseDown(mouseEvent);

    // Second handleCaretPositionChange call with same version - should clear ghost
    // texts but not trigger completion
    completionManager.mouseDown(mouseEvent);

    // Verify completion was never triggered since version didn't change
    verify(mockCompletionProvider, never()).triggerCompletion(any(), any(), any(Integer.class),any(Boolean.class));
    assertEquals(1, completionManager.getDocumentVersion());
  }

  @Test
  void testAutoCompletionDisabledDoesNotTriggerCompletion() {
    // Disable auto completion via property change (the production code path)
    completionManager.propertyChange(new PropertyChangeEvent(
        mockSettingsManager, Constants.AUTO_SHOW_COMPLETION, "true", "false"));

    // Setup version change
    when(mockLsConnection.getDocumentVersion(documentUri)).thenReturn(1, 2);

    MouseEvent mouseEvent = createMockMouseEvent();
    completionManager.mouseDown(mouseEvent); // Initialize
    completionManager.setModelOffset(10);
    completionManager.mouseDown(mouseEvent); // Should not trigger due to disabled auto completion

    // Verify completion was never triggered
    verify(mockCompletionProvider, never()).triggerCompletion(any(), any(), any(Integer.class), any(Boolean.class));
  }

  private MouseEvent createMockMouseEvent() {
    MouseEvent event = mock(MouseEvent.class);
    event.widget = completionManager.getStyledText();
    return event;
  }

  /**
   * Testable subclass of BaseCompletionManager that provides access to protected
   * methods and implements the abstract methods needed for testing.
   */
  private static class TestableBaseCompletionManager extends BaseCompletionManager {

    private int modelOffset;

    public TestableBaseCompletionManager(CopilotLanguageServerConnection lsConnection, CompletionProvider provider,
        ITextEditor editor, LanguageServerSettingManager settingsManager) {
      super(lsConnection, provider, editor, settingsManager);
    }

    @Override
    protected void updateGhostTexts(org.eclipse.jface.text.Position inferredPosition) {
      // Mock implementation for testing
    }

    @Override
    public void clearGhostTexts() {
      // Mock implementation for testing
    }

    // Expose protected/package-private fields for testing
    public int getDocumentVersion() {
      return this.documentVersion;
    }

    public StyledText getStyledText() {
      return this.styledText;
    }

    @Override
    protected int getModelOffsetFromCaretPosition() {
      return modelOffset;
    }

    public void setModelOffset(int modelOffset) {
      this.modelOffset = modelOffset;
    }
  }
}