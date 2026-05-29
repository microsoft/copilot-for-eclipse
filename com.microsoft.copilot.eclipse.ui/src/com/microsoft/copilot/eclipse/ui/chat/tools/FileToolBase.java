// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
import org.eclipse.core.runtime.Status;
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
  protected static Map<ChangedFile, CompareEditorInput> compareEditorInputMap = new ConcurrentHashMap<>();
  protected static Map<ChangedFile, String> fileContentCache = new ConcurrentHashMap<>();

  @Override
  public abstract CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView);

  /**
   * Common method to handle cleanup of file changes.
   */
  protected void cleanupChangedFiles() {
    for (ChangedFile file : compareEditorInputMap.keySet()) {
      closeCompareEditor(file);
    }
    compareEditorInputMap.clear();
    fileContentCache.clear();
  }

  /**
   * Caches the original content of the changed file to be compared with the proposed changes.
   *
   * @param file The changed file whose original content is to be cached.
   */
  protected void cacheTheOriginalFileContent(ChangedFile file) {
    if (fileContentCache.containsKey(file)) {
      // We only need to cache the original file content once to keep the initial file content so that we can undo the
      // entire file edit even the file has been modified for multiple rounds.
      return;
    }
    try {
      fileContentCache.put(file, readCurrentFileContent(file));
    } catch (IOException | CoreException e) {
      CopilotCore.LOGGER.error("Error caching original file content", e);
    }
  }

  /**
   * Caches the original content for a changed file if no baseline exists yet.
   *
   * @param file The file whose original content is to be cached.
   * @param content The content to use as the original baseline.
   */
  protected void cacheTheOriginalFileContent(ChangedFile file, String content) {
    fileContentCache.putIfAbsent(file, content);
  }

  private String readCurrentFileContent(ChangedFile file) throws IOException, CoreException {
    if (file.isWorkspaceFile()) {
      IFile workspaceFile = file.getWorkspaceFile();
      try (InputStream inputStream = workspaceFile.getContents()) {
        return new String(inputStream.readAllBytes(), PlatformUtils.getFileCharset(workspaceFile));
      }
    }
    return Files.readString(file.getLocalPath(), StandardCharsets.UTF_8);
  }

  /**
   * Gets the cached original content for a changed file.
   *
   * @param file The changed file whose cached content should be returned.
   * @return the cached content, or null if no content is cached.
   */
  protected String getCachedFileContent(ChangedFile file) {
    return fileContentCache.get(file);
  }

  /**
   * Removes the cached original content for a changed file.
   *
   * @param file The changed file whose cached content should be removed.
   */
  protected void removeCachedFileContent(ChangedFile file) {
    fileContentCache.remove(file);
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
   * Compares the given string with the content of a changed file in a compare editor.
   *
   * @param originalFileContent The original string content of the file to compare with.
   * @param file The changed file with the proposed changes applied.
   */
  protected void compareStringWithFile(String originalFileContent, ChangedFile file) {
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
   * Refreshes the compare editor for the given changed file only if it is already open.
   *
   * @param fileContent The original file content to compare against.
   * @param file The changed file whose compare editor should be refreshed.
   */
  protected void refreshCompareEditorIfOpen(String fileContent, ChangedFile file) {
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
        }
        CompareEditorInput compareEditorInput = compareEditorInputMap.get(file);
        if (compareEditorInput != null) {
          CompareUI.reuseCompareEditor(compareEditorInput, (IReusableEditor) editor);
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
   * Closes the compare editor for the given changed file if it is open.
   *
   * @param file The changed file to check.
   */
  protected void closeCompareEditor(ChangedFile file) {
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
   * Brings the compare editor for a changed file to the top if it is open.
   *
   * @param file The changed file whose compare editor should be shown.
   * @return true if an open compare editor was found, false otherwise.
   */
  protected boolean bringCompareEditorToTopIfOpen(ChangedFile file) {
    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input == null) {
      return false;
    }
    if (isCompareEditorOpen(input)) {
      bringCompareEditorToTop(input);
      return true;
    }
    compareEditorInputMap.remove(file);
    return false;
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
   * Resolves an absolute local filesystem path from a path or file URI.
   *
   * @param filePath the path or URI to resolve
   * @return the local filesystem path, or null if the input is not an absolute local path
   */
  protected Path getLocalFilePath(String filePath) {
    try {
      if (filePath.startsWith("file:")) {
        return Paths.get(new URI(filePath));
      }
      Path path = Paths.get(filePath);
      return path.isAbsolute() ? path : null;
    } catch (IllegalArgumentException | URISyntaxException e) {
      CopilotCore.LOGGER.error("Invalid local file path: " + filePath, e);
      return null;
    }
  }

  private CompareEditorInput createWorkspaceCompareEditorInput(String comparedContent, IFile file) {
    ChangedFile changedFile = ChangedFile.workspace(file);
    EditableFileCompareInput originalFile = new EditableFileCompareInput(file);
    return createCompareEditorInputForTarget(comparedContent, originalFile.getName(), originalFile.getType(),
        PlatformUtils.getFileCharset(file), () -> originalFile, (diffNode, monitor) -> {
          EditableFileCompareInput inputToBeApplied = (EditableFileCompareInput) diffNode.getLeft();
          try (InputStream inputStream = inputToBeApplied.getContents()) {
            file.setContents(inputStream, true, true, monitor);
          } catch (IOException e) {
            CopilotCore.LOGGER.error("Error saving compare editor changes to file", e);
          }
          CopilotUi.getPlugin().getChatServiceManager().getFileToolService().completeFile(changedFile);
          removeCachedFileContent(changedFile);
        });
  }

  private CompareEditorInput createLocalCompareEditorInput(String comparedContent, Path file) {
    Path normalizedPath = normalizeLocalPath(file);
    ChangedFile changedFile = ChangedFile.local(normalizedPath);
    EditableFileCompareInput originalFile = new EditableFileCompareInput(normalizedPath);
    return createCompareEditorInputForTarget(comparedContent, originalFile.getName(), originalFile.getType(),
        StandardCharsets.UTF_8.name(), () -> originalFile,
        (diffNode, monitor) -> {
          EditableFileCompareInput inputToBeApplied = (EditableFileCompareInput) diffNode.getLeft();
          try (InputStream inputStream = inputToBeApplied.getContents()) {
            Files.write(normalizedPath, inputStream.readAllBytes());
          } catch (IOException e) {
            CopilotCore.LOGGER.error("Error saving compare editor changes to local file", e);
          }
          CopilotUi.getPlugin().getChatServiceManager().getFileToolService().completeFile(changedFile);
          removeCachedFileContent(changedFile);
        });
  }

  private CompareEditorInput createCompareEditorInput(String comparedContent, ChangedFile file) {
    if (file.isWorkspaceFile()) {
      return createWorkspaceCompareEditorInput(comparedContent, file.getWorkspaceFile());
    }
    return createLocalCompareEditorInput(comparedContent, file.getLocalPath());
  }

  private CompareEditorInput createCompareEditorInputForTarget(String comparedContent, String fileName,
      String fileExtension, String charset, Supplier<ITypedElement> originalFileSupplier,
      CompareContentSaver contentSaver) {
    CompareConfiguration config = createCompareConfiguration(fileName);

    return new CompareEditorInput(config) {
      @Override
      protected Object prepareInput(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Calculating differences", 10);
        setTitle(Messages.agent_tool_compareEditor_titlePrefix + fileName);
        EditableStringCompareInput proposedChanges = new EditableStringCompareInput(comparedContent, fileName,
            fileExtension, charset);
        DiffNode diffNode = new DiffNode(null, Differencer.CHANGE, null, originalFileSupplier.get(), proposedChanges);
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
            contentSaver.save(diffNode, monitor);
          }
        }
      }
    };
  }

  private CompareConfiguration createCompareConfiguration(String rightLabel) {
    CompareConfiguration config = new CompareConfiguration();
    config.setLeftLabel(Messages.agent_tool_compareEditor_proposedChangesTitle.replaceAll("\"", ""));
    config.setRightLabel(rightLabel);
    config.setLeftEditable(true);
    config.setRightEditable(false);
    config.setProperty(CompareConfiguration.USE_OUTLINE_VIEW, Boolean.TRUE);
    config.setProperty(CompareConfiguration.SHOW_PSEUDO_CONFLICTS, Boolean.TRUE);
    config.setProperty(CompareConfiguration.IGNORE_WHITESPACE, Boolean.FALSE);
    return config;
  }

  /**
   * Saves the editable compare content back to the target file type.
   */
  @FunctionalInterface
  private interface CompareContentSaver {
    /**
     * Saves the edited content represented by a compare diff node.
     *
     * @param diffNode The diff node containing the editable compare inputs.
     * @param monitor The progress monitor for the save operation.
     * @throws CoreException if saving through Eclipse APIs fails.
     */
    void save(DiffNode diffNode, IProgressMonitor monitor) throws CoreException;
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
  public static final class EditableFileCompareInput implements ITypedElement, IEncodedStreamContentAccessor,
      IEditableContent {
    private final IFile workspaceFile;
    private final Path localFile;
    private byte[] modifiedContent = null;

    /**
     * Constructor for EditableFileCompareInput.
     *
     * @param file The file to be edited.
     */
    public EditableFileCompareInput(IFile file) {
      this.workspaceFile = file;
      this.localFile = null;
    }

    /**
     * Constructor for EditableFileCompareInput.
     *
     * @param file The local file to be edited.
     */
    EditableFileCompareInput(Path file) {
      this.workspaceFile = null;
      this.localFile = file.toAbsolutePath().normalize();
    }

    @Override
    public String getName() {
      if (workspaceFile != null) {
        return workspaceFile.getName();
      }
      Path fileName = localFile.getFileName();
      return fileName == null ? localFile.toString() : fileName.toString();
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      if (workspaceFile != null) {
        return workspaceFile.getFileExtension();
      }
      return getLocalFileExtension(localFile);
    }

    /**
     * Gets the workspace file represented by this compare input.
     *
     * @return the workspace file
     */
    public IFile getFile() {
      return workspaceFile;
    }

    @Override
    public InputStream getContents() throws CoreException {
      if (modifiedContent != null) {
        return new ByteArrayInputStream(modifiedContent);
      }
      if (workspaceFile != null) {
        return workspaceFile.getContents();
      }
      try {
        return Files.newInputStream(localFile);
      } catch (IOException e) {
        throw new CoreException(Status.error("Error reading local file", e));
      }
    }

    @Override
    public String getCharset() throws CoreException {
      return workspaceFile == null ? StandardCharsets.UTF_8.name() : workspaceFile.getCharset();
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
          modifiedContent = is.readAllBytes();
        } catch (IOException | CoreException e) {
          CopilotCore.LOGGER.error("Error occurred while replacing file content", e);
        }
      }
      return this;
    }

    private static String getLocalFileExtension(Path file) {
      String name = file.getFileName() == null ? file.toString() : file.getFileName().toString();
      int index = name.lastIndexOf('.');
      if (index < 0 || index == name.length() - 1) {
        return "";
      }
      return name.substring(index + 1);
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
