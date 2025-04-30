package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.FileChangeSummaryBar;
import com.microsoft.copilot.eclipse.ui.chat.NewConversationListener;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Tool for editing files.
 */
public class EditFileTool extends BaseTool implements FileChangeSummaryHandler, NewConversationListener {
  private static final String TOOL_NAME = "insert_edit_into_file";
  private Map<IFile, CompareEditorInput> compareEditorInputMap;
  private Map<IFile, String> fileContentCache;
  private FileChangeSummaryBar fileChangeSummaryBar;

  /**
   * Constructor for EditFileTool.
   */
  public EditFileTool() {
    this.name = TOOL_NAME;
    this.compareEditorInputMap = new ConcurrentHashMap<>();
    this.fileContentCache = new ConcurrentHashMap<>();
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Insert new code into an existing file in the workspace.
        Use this tool once per file that needs to be modified, even if there are multiple changes for a file.
        Generate the "explanation" property first.
        The system is very smart and can understand how to apply your edits to the files,
        you just need to provide minimal hints.
        Avoid repeating existing code, instead use comments to represent regions of unchanged code.
        Be as concise as possible.
        For example:
        // ...existing code...
        { changed code }
        // ...existing code...
        { changed code }
        // ...existing code...
        Here is an example of how you should use format an edit to an existing Person class:
        class Person {
        \t// ...existing code...
        \tage: number;
        \t// ...existing code...
        \tgetAge() {
        \treturn this.age;
        \t}
        }
        """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put("explanation",
        new InputSchemaPropertyValue("string", "A short explanation of the edit being made."));
    properties.put("filePath", new InputSchemaPropertyValue("string", "An absolute path to the file to edit."));
    properties.put("code", new InputSchemaPropertyValue("string", """
        The code change to apply to the file.
        The system is very smart and can understand how to apply your edits to the files,
        you just need to provide minimal hints.
        Avoid repeating existing code, instead use comments to represent regions of unchanged code.
        Be as concise as possible.
        For example:
        // ...existing code...
        { changed code }
        // ...existing code...
        { changed code }
        // ...existing code...
        Here is an example of how you should use format an edit to an existing Person class:
        class Person {
        \t// ...existing code...
        \tage: number;
        \t// ...existing code...
        \tgetAge() {
        \t\treturn this.age;
        \t}
        }
        """));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(Arrays.asList("explanation", "filePath", "code"));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    // Implementation for invoking the edit file tool
    CompletableFuture<LanguageModelToolResult[]> resultFuture = new CompletableFuture<>();
    if (input.get("filePath") instanceof String filePath) {
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(filePath));
      if (file == null || !file.exists()) {
        resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
            "The file path provided does not exist. Please check the path and try again.") });
        return resultFuture;
      }

      if (input.get("code") instanceof String code) {
        createFileChangeSummaryBar(file, chatView);
        cacheTheOriginalFileContent(file);
        applyChangesToFile(code, file);
        updateOrCreateCompareStringWithFile(fileContentCache.get(file), file);

        // Must return the updated content as a result to the CLS.
        resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(code) });
      } else {
        resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
            "The code provided is not a valid string. Please check the code and try again.") });
      }
    } else {
      // TODO: May need to support multiple file paths in the future
      resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
          "The file path provided is not a valid string. Please check the path and try again.") });
    }
    return resultFuture;
  }

  /**
   * Compares the given string with the content of the given file in a compare editor.
   *
   * @param originalFileContent The original string content of the file to compare with.
   * @param file The user's file with the proposed changes has been applied.
   * @throws InvocationTargetException If the operation is canceled.
   * @throws InterruptedException If the operation is canceled.
   */
  private void compareStringWithFile(String originalFileContent, IFile file) {
    try {
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

      CompareEditorInput input = new CompareEditorInput(config) {
        @Override
        protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          monitor.beginTask("Calculating differences", 10);
          setTitle(Messages.agent_tool_compareEditor_TitlePrefix + file.getName());
          // Keep proposedChanges virtual file's name and type same as the originalFile original file's name and type
          EditableStringCompareInput proposedChanges = new EditableStringCompareInput(originalFileContent,
              file.getName(), file.getFileExtension());
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
            CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().completeFile(file);
            fileContentCache.remove(file);
          }
        }
      };
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

  private void updateOrCreateCompareStringWithFile(String originalFileContent, IFile file) {
    if (originalFileContent == null) {
      return;
    }

    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        CompareUI.reuseCompareEditor(input, (IReusableEditor) getCompareEditor(input));
      });
      bringCompareEditorToTop(input);
    } else {
      // If not, create a new compare editor
      compareStringWithFile(originalFileContent, file);
    }
  }

  private void applyChangesToFile(String changedContent, IFile file) {
    try {

      // Convert the string content to an input stream
      ByteArrayInputStream inputStream = new ByteArrayInputStream(changedContent.getBytes(StandardCharsets.UTF_8));

      // Set the file contents
      file.setContents(inputStream, true, true, new NullProgressMonitor());

      // Refresh the file to ensure Eclipse recognizes the changes
      file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

      // Close the input stream
      inputStream.close();
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error replacing file content", e);
    }
  }

  private void cacheTheOriginalFileContent(IFile file) {
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

  private boolean bringCompareEditorToTop(CompareEditorInput input) {
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

  private void closeCompareEditor(IFile file) {
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

  private void createFileChangeSummaryBar(IFile file, ChatView chatView) {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (fileChangeSummaryBar == null) {
        CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().setChangedFiles(new LinkedHashMap<>() {
          {
            put(file, false);
          }
        });
        fileChangeSummaryBar = new FileChangeSummaryBar(chatView.getMainSection(), SWT.NONE, this);
        chatView.registerNewConversationListenerToTheTopBanner(this);
      } else {
        CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().addChangedFile(file);
      }
      chatView.getMainSection().layout(true, true);
    });
  }

  @Override
  public void onKeepChange(IFile file) {
    CompareEditorInput input = compareEditorInputMap.get(file);
    boolean handled = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().getChangedFiles()
        .get(file);
    // Only process the file if it is not already handled
    if (input != null && !handled) {
      try {
        input.saveChanges(new NullProgressMonitor());
      } catch (CoreException e) {
        CopilotCore.LOGGER.error("Error saving changes to file", e);
      }
    }
    CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().completeFile(file);
    updateOrCreateCompareStringWithFile(fileContentCache.get(file), file);
    fileContentCache.remove(file);
  }

  @Override
  public void onKeepAllChanges() {
    Map<IFile, Boolean> files = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService()
        .getChangedFiles();
    for (IFile file : files.keySet()) {
      onKeepChange(file);
    }
  }

  @Override
  public void onUndoChange(IFile file) {
    undoChangesToFile(file);
    CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().completeFile(file);
  }

  @Override
  public void onUndoAllChanges() {
    Map<IFile, Boolean> files = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService()
        .getChangedFiles();
    for (IFile file : files.keySet()) {
      onUndoChange(file);
    }
  }

  @Override
  public void onRemoveFile(IFile file) {
    Map<IFile, Boolean> changedFiles = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService()
        .getChangedFiles();
    
    // If the file is not handled by user, we need to undo the changes made to the file before removing it.
    if (changedFiles.containsKey(file) && !changedFiles.get(file)) {
      undoChangesToFile(file);
    }
    CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().removeFile(file);
  }

  @Override
  public void onViewDiff(IFile file) {
    // Check if the file is already open in a compare editor and bring it to the top if is exists
    if (compareEditorInputMap.containsKey(file) && bringCompareEditorToTop(compareEditorInputMap.get(file))) {
      return;
    }
    // If the compare editor is created but closed, remove it from the map and create a new one
    compareEditorInputMap.remove(file);
    compareStringWithFile(fileContentCache.get(file), file);
  }

  @Override
  public void onAllChangesResolved() {
    cleanupChangedFiles(false);
  }

  @Override
  public void onNewConversation() {
    cleanupChangedFiles(true);
  }
  
  private void undoChangesToFile(IFile file) {  
    String fileCache = fileContentCache.get(file);
    boolean handled = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().getChangedFiles()
        .get(file);
    
    // Only process the file if it is not already handled
    if (fileCache != null && !handled) {
      applyChangesToFile(fileCache, file);
    }
    updateOrCreateCompareStringWithFile(fileContentCache.get(file), file);
    fileContentCache.remove(file);
  }

  /**
   * Common method to handle cleanup of file changes.
   *
   * @param undoChanges Whether to undo changes to each file or not.
   */
  private void cleanupChangedFiles(boolean undoChanges) {
    Map<IFile, Boolean> files = CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService()
        .getChangedFiles();
    for (IFile file : files.keySet()) {
      if (undoChanges) {
        // TODO: Add a confirmation dialog to ask the user if they want to keep or undo the changes
        onUndoChange(file);
      }
      closeCompareEditor(file);
    }
    CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().setChangedFiles(new LinkedHashMap<>());
    CopilotUi.getPlugin().getChatServiceManager().getEditFileToolService().setFileChangeSummaryBarButtonStatus(false);
    disposeFileChangeSummaryBar();
  }

  private void disposeFileChangeSummaryBar() {
    if (fileChangeSummaryBar != null) {
      fileChangeSummaryBar.dispose();
      fileChangeSummaryBar = null;
    }
    this.compareEditorInputMap.clear();
    this.fileContentCache.clear();
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

  // String input with edit support
  private class EditableStringCompareInput implements ITypedElement, IStreamContentAccessor, IEditableContent {
    private String content;
    private String name;
    private String type;

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
