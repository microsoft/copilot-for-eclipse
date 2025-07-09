package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.tools.FileToolService.FileChangeAction;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Abstract class for handling file change tool related actions.
 */
public abstract class FileToolBase extends BaseTool {
  protected static Map<IFile, CompareEditorInput> compareEditorInputMap = new ConcurrentHashMap<>();
  protected static Map<IFile, String> fileContentCache = new ConcurrentHashMap<>();

  @Override
  public abstract CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView);

  /**
   * Common method to handle cleanup of file changes.
   */
  protected void cleanupChangedFiles() {
    for (IFile file : compareEditorInputMap.keySet()) {
      closeCompareEditor(file);
    }
    compareEditorInputMap.clear();
    fileContentCache.clear();
  }

  /**
   * Caches the original content of the file to be compared with the proposed changes.
   *
   * @param file The file whose original content is to be cached.
   */
  protected void cacheTheOriginalFileContent(IFile file) {
    if (fileContentCache.containsKey(file)) {
      // We only need to cache the original file content once to keep the initial file content so that we can undo the
      // entire file edit even the file has been modified for multiple rounds.
      return;
    }
    try (InputStream inputStream = file.getContents()) {
      String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      fileContentCache.put(file, content);
    } catch (IOException | CoreException e) {
      CopilotCore.LOGGER.error("Error caching original file content", e);
    }
  }

  /**
   * Compares the given string with the content of the given file in a compare editor.
   *
   * @param originalFileContent The original string content of the file to compare with.
   * @param file The user's file with the proposed changes has been applied.
   * @throws InvocationTargetException If the operation is canceled.
   * @throws InterruptedException If the operation is canceled.
   */
  protected void compareStringWithFile(String originalFileContent, IFile file) {
    try {
      CompareEditorInput input = createCompareEditorInput(originalFileContent, file);
      input.run(new NullProgressMonitor());

      // TODO: Add a progress monitor to show the progress of the operation input.run(new NullProgressMonitor());
      compareEditorInputMap.put(file, input);
      SwtUtils.invokeOnDisplayThread(() -> {
        CompareUI.openCompareEditor(input);
      });
    } catch (InvocationTargetException | InterruptedException e) {
      CopilotCore.LOGGER.error("Error opening compare editor", e);
    }
  }

  /**
   * Updates the current or creates a new compare editor with the given file content and file.
   *
   * @param originalFileContent The original string content of the file to compare with.
   * @param file The user's file with the proposed changes has been applied.
   */
  protected void updateOrCreateCompareStringWithFile(String fileContent, IFile file) {
    if (fileContent == null) {
      return;
    }

    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input != null) {
      if (fileContent.equals(fileContentCache.get(file))) {
        SwtUtils.invokeOnDisplayThread(() -> {
          CompareUI.reuseCompareEditor(input, (IReusableEditor) getCompareEditor(input));
        });
      } else {
        CompareEditorInput newInput = createCompareEditorInput(fileContent, file);
        SwtUtils.invokeOnDisplayThread(() -> {
          CompareUI.reuseCompareEditor(newInput, (IReusableEditor) getCompareEditor(input));
        });
        compareEditorInputMap.put(file, newInput);
      }
      bringCompareEditorToTop(input);
    } else {
      // If not, create a new compare editor
      compareStringWithFile(fileContent, file);
    }
  }

  /**
   * Brings the compare editor to the top of the workbench.
   *
   * @param input The CompareEditorInput to be brought to the top.
   * @return true if the editor was successfully brought to the top, false otherwise.
   */
  protected boolean bringCompareEditorToTop(CompareEditorInput input) {
    AtomicReference<Boolean> ref = new AtomicReference<>(false);
    SwtUtils.invokeOnDisplayThread(() -> {
      IWorkbenchPage page = UiUtils.getActivePage();
      IEditorPart editor = getCompareEditor(input);
      if (editor != null) {
        page.bringToTop(editor);
        ref.set(true);
      }
    });
    return ref.get();
  }

  private IEditorPart getCompareEditor(CompareEditorInput input) {
    IWorkbenchPage page = UiUtils.getActivePage();
    if (page == null) {
      return null;
    }
    for (IEditorReference editorRef : page.getEditorReferences()) {
      IEditorPart editor = editorRef.getEditor(false);
      if (editor != null && editor.getEditorInput().equals(input)) {
        return editor;
      }
    }
    return null;
  }

  /**
   * Close the compare editor for the given file if it is open.
   *
   * @param file The file to check.
   * @return true if the compare editor is open, false otherwise.
   */
  protected void closeCompareEditor(IFile file) {
    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        IWorkbenchPage page = UiUtils.getActivePage();
        if (page == null) {
          return;
        }
        IEditorReference[] editorRefs = page.getEditorReferences();
        for (IEditorReference ref : editorRefs) {
          IEditorPart editor = ref.getEditor(false);
          if (editor != null && editor.getEditorInput() == input) {
            page.closeEditor(editor, false);
            break;
          }
        }
      });
    }
    compareEditorInputMap.remove(file);
  }

  private CompareEditorInput createCompareEditorInput(String comparedContent, IFile file) {
    // Create a new CompareConfiguration
    CompareConfiguration config = new CompareConfiguration();
    config.setLeftLabel(Messages.agent_tool_compareEditor_proposedChangesTitle.replaceAll("\"", ""));
    config.setRightLabel(file.getName());

    // Enable editing on the proposed changes side and disable it on the original file side. Eclipse's original side
    // and
    // changes side are swapped, so we need to set the left side as editable to edit the proposed changes.
    config.setLeftEditable(true);
    config.setRightEditable(false);

    // Set up the configuration to properly show differences
    config.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, Boolean.TRUE);
    config.setProperty(CompareConfiguration.SHOW_PSEUDO_CONFLICTS, Boolean.TRUE);
    config.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.FALSE);

    return new CompareEditorInput(config) {
      @Override
      protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Calculating differences", 10);
        setTitle(Messages.agent_tool_compareEditor_titlePrefix + file.getName());
        // Keep proposedChanges virtual file's name and type same as the originalFile original file's name and type
        EditableStringCompareInput proposedChanges = new EditableStringCompareInput(comparedContent, file.getName(),
            file.getFileExtension());
        EditableFileCompareInput originalFile = new EditableFileCompareInput(file);

        // Create a diff node with proper configuration for text comparison
        DiffNode diffNode = new DiffNode(null, Differencer.CHANGE, null, originalFile, proposedChanges);

        monitor.done();
        return diffNode;
      }

      @Override
      public void saveChanges(IProgressMonitor monitor) throws CoreException {
        // We need to set the right side as editable to save the changes made to the proposed changes. Otherwise, the
        // changes won't be saved.
        if (isDirty()) {
          config.setRightEditable(true);
          super.saveChanges(monitor);

          // Get the diff node which contains the comparison inputs
          DiffNode diffNode = (DiffNode) getCompareResult();
          if (diffNode != null) {
            // Get the right side input (the original file with any edits made)
            EditableFileCompareInput inputToBeApplied = (EditableFileCompareInput) diffNode.getLeft();

            // Save the modified content back to the file
            try (InputStream inputStream = inputToBeApplied.getContents()) {
              file.setContents(inputStream, true, true, monitor);
            } catch (IOException e) {
              CopilotCore.LOGGER.error("Error saving compare editor changes to file", e);
            }
          }

          // If user keeps the changes with keyboard shortcut, we also need to complete the file.
          CopilotUi.getPlugin().getChatServiceManager().getFileToolService().completeFile(file,
              FileChangeAction.ACCEPTED);
          fileContentCache.remove(file);
        }
      }
    };
  }

  /**
   * Dispose the file change summary bar and related resources.
   */
  protected void dispose() {
    if (compareEditorInputMap != null) {
      compareEditorInputMap.clear();
    }

    if (fileContentCache != null) {
      fileContentCache.clear();
    }
  }

  /**
   * Editable file compare input class to handle file content editing on the compare editor.
   */
  public class EditableFileCompareInput implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private IFile file;
    private byte[] modifiedContent = null;

    /**
     * Constructor for EditableFileCompareInput.
     *
     * @param file The file to be edited.
     */
    public EditableFileCompareInput(IFile file) {
      this.file = file;
    }

    @Override
    public String getName() {
      return file.getName();
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      return file.getFileExtension();
    }

    public IFile getFile() {
      return file;
    }

    @Override
    public InputStream getContents() throws CoreException {
      if (modifiedContent != null) {
        return new ByteArrayInputStream(modifiedContent);
      }
      return file.getContents();
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setContent(byte[] newContent) {
      this.modifiedContent = newContent;
    }

    @Override
    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
      if (src instanceof IStreamContentAccessor sca) {
        try (InputStream is = sca.getContents()) {
          // Just store changes in memory
          modifiedContent = is.readAllBytes();
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing file content", e);
        }
      }
      return this;
    }
  }

  /**
   * A class for the compare editor string input with edit support.
   */
  public class EditableStringCompareInput implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private String content;
    private String name;
    private String type;

    /**
     * Constructor for EditableStringCompareInput.
     *
     * @param content The content of the string.
     * @param name The name of the string.
     * @param type The type of the file, should be same as the compared file type.
     */
    public EditableStringCompareInput(String content, String name, String type) {
      this.content = content;
      this.name = name;
      this.type = type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public InputStream getContents() throws CoreException {
      return new ByteArrayInputStream(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setContent(byte[] newContent) {
      content = new String(newContent, StandardCharsets.UTF_8);
    }

    @Override
    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
      if (src instanceof IStreamContentAccessor sca) {
        try (InputStream is = sca.getContents()) {
          content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing string content", e);
        }
      }
      return this;
    }
  }
}
