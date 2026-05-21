// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPage;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Abstract class for handling file change tool related actions.
 */
public abstract class FileToolBase extends BaseTool {
  protected static Map<IFile, CompareEditorInput> compareEditorInputMap = new ConcurrentHashMap<>();
  protected static Map<IFile, String> fileContentCache = new ConcurrentHashMap<>();
  protected static Map<Path, CompareEditorInput> localCompareEditorInputMap = new ConcurrentHashMap<>();
  protected static Map<Path, String> localFileContentCache = new ConcurrentHashMap<>();

  @Override
  public abstract CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView);

  /**
   * Common method to handle cleanup of file changes.
   */
  protected void cleanupChangedFiles() {
    for (IFile file : compareEditorInputMap.keySet()) {
      closeCompareEditor(file);
    }
    for (Path file : localCompareEditorInputMap.keySet()) {
      closeCompareEditor(file);
    }
    compareEditorInputMap.clear();
    fileContentCache.clear();
    localCompareEditorInputMap.clear();
    localFileContentCache.clear();
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
      String content = new String(inputStream.readAllBytes(), PlatformUtils.getFileCharset(file));
      fileContentCache.put(file, content);
    } catch (IOException | CoreException e) {
      CopilotCore.LOGGER.error("Error caching original file content", e);
    }
  }

  /**
   * Caches the original content for a workspace file if no baseline exists yet.
   *
   * @param file The file whose original content is to be cached.
   * @param content The content to use as the original baseline.
   */
  protected void cacheTheOriginalFileContent(IFile file, String content) {
    fileContentCache.putIfAbsent(file, content);
  }

  /**
   * Caches the original content of a local file to be compared with the proposed changes.
   *
   * @param file The local file whose original content is to be cached.
   */
  protected void cacheTheOriginalFileContent(Path file) {
    Path normalizedPath = normalizeLocalPath(file);
    if (localFileContentCache.containsKey(normalizedPath)) {
      return;
    }
    try {
      localFileContentCache.put(normalizedPath, Files.readString(normalizedPath, StandardCharsets.UTF_8));
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Error caching original local file content", e);
    }
  }

  /**
   * Caches the original content for a local file if no baseline exists yet.
   *
   * @param file The local file whose original content is to be cached.
   * @param content The content to use as the original baseline.
   */
  protected void cacheTheOriginalFileContent(Path file, String content) {
    localFileContentCache.putIfAbsent(normalizeLocalPath(file), content);
  }

  /**
   * Validate the edit to ensure the files are writable.
   *
   * @throws CoreException If the validation fails.
   */
  protected boolean validateEdit(IFile file) throws CoreException {
    final boolean[] result = new boolean[] { false };
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { file }, null);
        if (status != null && status.isOK()) {
          result[0] = true;
        }
      }
    }, new NullProgressMonitor());
    return result[0];
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
      compareEditorInputMap.put(file, input);
      // TODO: Add a progress monitor to show the progress of the operation input.run(new NullProgressMonitor());
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        CompareEditorInput compareEditorInput = compareEditorInputMap.get(file);
        if (compareEditorInput != null) {
          CompareUI.openCompareEditor(compareEditorInput);
        }
      });
    } catch (InvocationTargetException | InterruptedException e) {
      CopilotCore.LOGGER.error("Error opening compare editor", e);
    }
  }

  /**
   * Compares the given string with the content of the given local file in a compare editor.
   *
   * @param originalFileContent The original string content of the file to compare with.
   * @param file The local file with the proposed changes applied.
   */
  protected void compareStringWithFile(String originalFileContent, Path file) {
    Path normalizedPath = normalizeLocalPath(file);
    try {
      CompareEditorInput input = createCompareEditorInput(originalFileContent, normalizedPath);
      input.run(new NullProgressMonitor());
      localCompareEditorInputMap.put(normalizedPath, input);
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        CompareEditorInput compareEditorInput = localCompareEditorInputMap.get(normalizedPath);
        if (compareEditorInput != null) {
          CompareUI.openCompareEditor(compareEditorInput);
        }
      });
    } catch (InvocationTargetException | InterruptedException e) {
      CopilotCore.LOGGER.error("Error opening local file compare editor", e);
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
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          CompareUI.reuseCompareEditor(input, (IReusableEditor) getCompareEditor(input));
        });
      } else {
        CompareEditorInput newInput = createCompareEditorInput(fileContent, file);
        compareEditorInputMap.put(file, newInput);
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          CompareEditorInput compareEditorInput = compareEditorInputMap.get(file);
          if (compareEditorInput != null) {
            CompareUI.reuseCompareEditor(compareEditorInput, (IReusableEditor) getCompareEditor(compareEditorInput));
          }
        });
      }
      bringCompareEditorToTop(input);
    } else {
      // If not, create a new compare editor
      compareStringWithFile(fileContent, file);
    }
  }

  /**
   * Refreshes the compare editor for the given file only if it is already open. Does not open a new editor or steal
   * focus.
   *
   * @param fileContent The original file content to compare against.
   * @param file The file whose compare editor should be refreshed.
   */
  protected void refreshCompareEditorIfOpen(String fileContent, IFile file) {
    if (fileContent == null) {
      return;
    }
    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input != null) {
      CompareEditorInput newInput = createCompareEditorInput(fileContent, file);
      compareEditorInputMap.put(file, newInput);
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        IEditorPart editor = getCompareEditor(input);
        if (editor == null) {
          // If the compare editor is closed, remove the input from the map and skip refreshing.
          compareEditorInputMap.remove(file);
          return;
        } else {
          CompareEditorInput compareEditorInput = compareEditorInputMap.get(file);
          if (compareEditorInput != null) {
            CompareUI.reuseCompareEditor(compareEditorInput, (IReusableEditor) editor);
          }
        }
      });
    }
  }

  /**
   * Refreshes the compare editor for the given local file only if it is already open. Does not open a new editor or
   * steal focus.
   *
   * @param fileContent The original file content to compare against.
   * @param file The local file whose compare editor should be refreshed.
   */
  protected void refreshCompareEditorIfOpen(String fileContent, Path file) {
    if (fileContent == null) {
      return;
    }
    Path normalizedPath = normalizeLocalPath(file);
    CompareEditorInput input = localCompareEditorInputMap.get(normalizedPath);
    if (input != null) {
      CompareEditorInput newInput = createCompareEditorInput(fileContent, normalizedPath);
      localCompareEditorInputMap.put(normalizedPath, newInput);
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        IEditorPart editor = getCompareEditor(input);
        if (editor == null) {
          localCompareEditorInputMap.remove(normalizedPath);
          return;
        } else {
          CompareEditorInput compareEditorInput = localCompareEditorInputMap.get(normalizedPath);
          if (compareEditorInput != null) {
            CompareUI.reuseCompareEditor(compareEditorInput, (IReusableEditor) editor);
          }
        }
      });
    }
  }

  /**
   * Brings the compare editor to the top of the workbench.
   *
   * @param input The CompareEditorInput to be brought to the top.
   */
  protected void bringCompareEditorToTop(CompareEditorInput input) {
    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      IWorkbenchPage page = UiUtils.getActivePage();
      IEditorPart editor = getCompareEditor(input);
      if (editor != null) {
        page.bringToTop(editor);
      }
    });
  }

  /**
   * Checks whether the compare editor for the given input is still open.
   *
   * @param input The CompareEditorInput to check.
   * @return true if the editor is open, false otherwise.
   */
  protected boolean isCompareEditorOpen(CompareEditorInput input) {
    AtomicReference<Boolean> isOpen = new AtomicReference<>(false);
    SwtUtils.invokeOnDisplayThread(() -> isOpen.set(getCompareEditor(input) != null));
    return isOpen.get();
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

  /**
   * Close the compare editor for the given local file if it is open.
   *
   * @param file The local file to check.
   */
  protected void closeCompareEditor(Path file) {
    Path normalizedPath = normalizeLocalPath(file);
    CompareEditorInput input = localCompareEditorInputMap.get(normalizedPath);
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
    localCompareEditorInputMap.remove(normalizedPath);
  }

  /**
   * Normalizes a local path for cache and map lookups.
   *
   * @param file the local file path
   * @return the normalized absolute path
   */
  protected Path normalizeLocalPath(Path file) {
    return file.toAbsolutePath().normalize();
  }

  /**
   * Gets the file extension for a local path.
   *
   * @param file the local file path
   * @return the extension without the dot, or an empty string if none exists
   */
  private String getLocalFileExtension(Path file) {
    String name = file.getFileName() == null ? file.toString() : file.getFileName().toString();
    int index = name.lastIndexOf('.');
    if (index < 0 || index == name.length() - 1) {
      return "";
    }
    return name.substring(index + 1);
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
            file.getFileExtension(), PlatformUtils.getFileCharset(file));
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
          CopilotUi.getPlugin().getChatServiceManager().getFileToolService().completeFile(file);
          fileContentCache.remove(file);
        }
      }
    };
  }

  private CompareEditorInput createCompareEditorInput(String comparedContent, Path file) {
    Path normalizedPath = normalizeLocalPath(file);
    String fileName = normalizedPath.getFileName() == null ? normalizedPath.toString()
        : normalizedPath.getFileName().toString();
    CompareConfiguration config = new CompareConfiguration();
    config.setLeftLabel(Messages.agent_tool_compareEditor_proposedChangesTitle.replaceAll("\"", ""));
    config.setRightLabel(fileName);
    config.setLeftEditable(true);
    config.setRightEditable(false);
    config.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, Boolean.TRUE);
    config.setProperty(CompareConfiguration.SHOW_PSEUDO_CONFLICTS, Boolean.TRUE);
    config.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.FALSE);

    return new CompareEditorInput(config) {
      @Override
      protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Calculating differences", 10);
        setTitle(Messages.agent_tool_compareEditor_titlePrefix + fileName);
        EditableStringCompareInput proposedChanges = new EditableStringCompareInput(comparedContent, fileName,
            getLocalFileExtension(normalizedPath), StandardCharsets.UTF_8.name());
        EditableLocalFileCompareInput originalFile = new EditableLocalFileCompareInput(normalizedPath);
        DiffNode diffNode = new DiffNode(null, Differencer.CHANGE, null, originalFile, proposedChanges);
        monitor.done();
        return diffNode;
      }

      @Override
      public void saveChanges(IProgressMonitor monitor) throws CoreException {
        if (isDirty()) {
          config.setRightEditable(true);
          super.saveChanges(monitor);

          DiffNode diffNode = (DiffNode) getCompareResult();
          if (diffNode != null) {
            EditableLocalFileCompareInput inputToBeApplied = (EditableLocalFileCompareInput) diffNode.getLeft();
            try (InputStream inputStream = inputToBeApplied.getContents()) {
              Files.write(normalizedPath, inputStream.readAllBytes());
            } catch (IOException e) {
              CopilotCore.LOGGER.error("Error saving compare editor changes to local file", e);
            }
          }

          CopilotUi.getPlugin().getChatServiceManager().getFileToolService().completeFile(normalizedPath);
          localFileContentCache.remove(normalizedPath);
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
    if (localCompareEditorInputMap != null) {
      localCompareEditorInputMap.clear();
    }
    if (localFileContentCache != null) {
      localFileContentCache.clear();
    }
  }

  /**
   * Editable file compare input class to handle file content editing on the compare editor.
   */
  public class EditableFileCompareInput implements ITypedElement, IEncodedStreamContentAccessor, IEditableContent {
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
    public String getCharset() throws CoreException {
      return file.getCharset();
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
  public class EditableStringCompareInput implements ITypedElement, IEncodedStreamContentAccessor, IEditableContent {
    private String content;
    private String name;
    private String type;
    private String charset;

    /**
     * Constructor for EditableStringCompareInput.
     *
     * @param content The content of the string.
     * @param name The name of the string.
     * @param type The type of the file, should be same as the compared file type.
     * @param charset The charset to use for encoding/decoding the content.
     */
    public EditableStringCompareInput(String content, String name, String type, String charset) {
      this.content = content;
      this.name = name;
      this.type = type;
      this.charset = charset;
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
      if (content == null) {
        return new ByteArrayInputStream(new byte[0]);
      }
      try {
        return new ByteArrayInputStream(content.getBytes(charset));
      } catch (UnsupportedEncodingException e) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
      }
    }

    @Override
    public String getCharset() throws CoreException {
      return charset;
    }

    @Override
    public boolean isEditable() {
      return true;
    }

    @Override
    public void setContent(byte[] newContent) {
      try {
        content = new String(newContent, charset);
      } catch (UnsupportedEncodingException e) {
        content = new String(newContent, StandardCharsets.UTF_8);
      }
    }

    @Override
    public ITypedElement replace(ITypedElement dest, ITypedElement src) {
      if (src instanceof IStreamContentAccessor sca) {
        try (InputStream is = sca.getContents()) {
          try {
            content = new String(is.readAllBytes(), charset);
          } catch (UnsupportedEncodingException e) {
            // Fallback to UTF-8 if charset is invalid
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
          }
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing string content", e);
        }
      }
      return this;
    }
  }
}
