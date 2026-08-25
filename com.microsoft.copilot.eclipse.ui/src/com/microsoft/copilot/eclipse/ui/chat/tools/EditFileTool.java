// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.semantic.ISemanticFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Tool for editing files.
 */
public class EditFileTool extends FileToolBase implements WorkingSetHandler {
  public static final String TOOL_NAME = "insert_edit_into_file";

  /**
   * Constructor for EditFileTool.
   */
  public EditFileTool() {
    super();
    this.name = TOOL_NAME;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Insert new code into an existing workspace file or local filesystem file.
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
    CompletableFuture<LanguageModelToolResult[]> resultFuture = new CompletableFuture<>();
    if (input.get("filePath") instanceof String filePath) {
      if (input.get("code") instanceof String code) {
        resultFuture.complete(editFile(filePath, code));
      } else {
        resultFuture.complete(new LanguageModelToolResult[] {
            new LanguageModelToolResult("The code provided is not a valid string. Please check the code and try again.",
                ToolInvocationStatus.error) });
      }
    } else {
      // TODO: May need to support multiple file paths in the future
      resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
          "The file path provided is not a valid string. Please check the path and try again.",
          ToolInvocationStatus.error) });
    }
    return resultFuture;
  }

  private LanguageModelToolResult[] editFile(String filePath, String code) {
    IFile file = FileUtils.getFileFromPath(filePath, true);

    if (file != null && file.exists()) {
      return editWorkspaceFile(file, code);
    }

    Path localPath = FileUtils.getLocalFilePath(filePath);
    if (localPath != null && Files.isRegularFile(localPath, LinkOption.NOFOLLOW_LINKS)) {
      return editLocalFile(localPath, code);
    }

    return new LanguageModelToolResult[] {
        new LanguageModelToolResult("The file path provided does not exist. Please check the path and try again.",
            ToolInvocationStatus.error) };
  }

  private LanguageModelToolResult[] editWorkspaceFile(IFile file, String code) {
    ChangedFile changedFile = ChangedFile.workspace(file);
    CopilotUi.getPlugin().getChatServiceManager().getFileToolService().addChangedFile(changedFile,
        FileChangeType.Changed);
    cacheTheOriginalFileContent(changedFile);
    try {
      applyChangesToFile(code, file);
    } catch (CoreException | IOException e) {
      CopilotCore.LOGGER.error("Error replacing file content", e);
      return new LanguageModelToolResult[] { new LanguageModelToolResult(
          "Failed to apply changes to the file: " + e.getMessage(), ToolInvocationStatus.error) };
    }
    refreshCompareEditorIfOpen(getCachedFileContent(changedFile), changedFile);
    return new LanguageModelToolResult[] { new LanguageModelToolResult(code, ToolInvocationStatus.success) };
  }

  private LanguageModelToolResult[] editLocalFile(Path filePath, String code) {
    Path normalizedPath = normalizeLocalPath(filePath);
    ChangedFile changedFile = ChangedFile.local(normalizedPath);
    try {
      String originalContent = getCachedFileContent(changedFile);
      if (originalContent == null) {
        originalContent = Files.readString(normalizedPath, StandardCharsets.UTF_8);
      }
      Files.writeString(normalizedPath, code, StandardCharsets.UTF_8);
      cacheTheOriginalFileContent(changedFile, originalContent);
      CopilotUi.getPlugin().getChatServiceManager().getFileToolService().addChangedFile(changedFile,
          FileChangeType.Changed);
      refreshCompareEditorIfOpen(getCachedFileContent(changedFile), changedFile);
      return new LanguageModelToolResult[] { new LanguageModelToolResult(code, ToolInvocationStatus.success) };
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Error replacing local file content", e);
      return new LanguageModelToolResult[] { new LanguageModelToolResult(
          "Failed to apply changes to the file: " + e.getMessage(), ToolInvocationStatus.error) };
    }
  }

  private void applyChangesToFile(String changedContent, IFile file) throws CoreException, IOException {
    if (!validateEdit(file)) {
      throw new IllegalStateException("File validation failed for " + file.getFullPath());
    }
    verifyTransportRequestForAdtLock(file);

    ByteArrayInputStream inputStream = getInputStream(changedContent, file);

    // Set the file contents
    file.setContents(inputStream, true, true, new NullProgressMonitor());

    // Refresh the file to ensure Eclipse recognizes the changes
    file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

    // Close the input stream
    inputStream.close();

    var buffer = FileBuffers.getTextFileBufferManager().getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
    if (buffer != null && buffer.isDirty()) {
      // Some editors (e.g. the ABAP source editor) do not listen for changes to the underlying file and
      // therefore leave a dirty, out-of-date buffer after we have written the new contents to disk. Force
      // the buffer to reload from disk by reverting it, so the open editor reflects the edit we just applied.
      Display.getDefault().asyncExec(() -> {
        try {
          buffer.revert(new NullProgressMonitor());
        } catch (CoreException e) {
          CopilotCore.LOGGER.error(e);
        }
      });
    }
  }

  private static final String ADT_LOCK_RESULT_CLASS = "com.sap.adt.tools.core.internal.locking.AdtLockResult";
  private static final QualifiedName ADT_LOCK_RESULT_PROPERTY = new QualifiedName("com.sap.adt.tools.filesystem",
      "LockResult");

  /**
   * When an ADT (ABAP Development Tools) file is locked in a transport-relevant way, it must be associated with a
   * transport request before it can be edited. The lock result is stored as a session property on the semantic file
   * and is accessed reflectively, as the ADT classes are not available at compile time.
   *
   * <p>
   * A distinction is made between the lock information being read successfully (in which case the transport
   * request is enforced) and the reflective read failing, e.g. because the ADT API changed. In the latter case
   * the check cannot be performed reliably, so the failure is logged and the edit is allowed to continue rather
   * than blocking the user.
   *
   * @param file the file about to be changed
   * @throws CoreException if the file is transport-relevant but has no transport request number assigned
   */
  private void verifyTransportRequestForAdtLock(IFile file) throws CoreException {
    var semanticFile = file.getAdapter(ISemanticFile.class);
    if (semanticFile == null) {
      return;
    }
    Object lockResult = semanticFile.getSessionProperty(ADT_LOCK_RESULT_PROPERTY);
    if (lockResult == null || !ADT_LOCK_RESULT_CLASS.equals(lockResult.getClass().getCanonicalName())) {
      return;
    }
    try {
      Boolean transportRelevant = readField(lockResult, "transportRelevant", Boolean.class);
      if (!Boolean.TRUE.equals(transportRelevant)) {
        return;
      }
      String transportRequestNumber = readField(lockResult, "transportRequestNumber", String.class);
      if (transportRequestNumber == null || transportRequestNumber.isEmpty()) {
        throw new CoreException(Status.error(String.format(
            "Cannot edit %s: the file is transport-relevant but no transport request number is assigned.",
            file.getFullPath())));
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      // The lock result is a genuine AdtLockResult, but its fields could not be read reflectively (e.g. the ADT
      // API changed or setAccessible was denied). The transport check cannot be performed reliably, so log the
      // failure and allow the edit to continue instead of blocking the user.
      CopilotCore.LOGGER.error("Could not verify ADT transport lock for " + file.getFullPath()
          + "; allowing the edit to continue.", e);
    }
  }

  private <T> T readField(Object target, String fieldName, Class<T> type) throws ReflectiveOperationException {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return type.cast(field.get(target));
  }

  private ByteArrayInputStream getInputStream(String changedContent, IFile file) {
    ByteArrayInputStream inputStream;
    try {
      inputStream = new ByteArrayInputStream(changedContent.getBytes(PlatformUtils.getFileCharset(file)));
    } catch (UnsupportedEncodingException e) {
      // Fallback to UTF-8 if the file charset is not supported
      CopilotCore.LOGGER.error("Unsupported encoding for file " + file.getFullPath() + ", falling back to UTF-8", e);
      inputStream = new ByteArrayInputStream(changedContent.getBytes(StandardCharsets.UTF_8));
    }
    return inputStream;
  }

  @Override
  public void onKeepChange(ChangedFile file) {
    removeCachedFileContent(file);
    closeCompareEditor(file);
  }

  @Override
  public void onUndoChange(ChangedFile file) throws CoreException, IOException {
    undoChangesToFile(file);
    closeCompareEditor(file);
  }

  @Override
  public void onViewDiff(ChangedFile file) {
    if (bringCompareEditorToTopIfOpen(file)) {
      return;
    }
    compareStringWithFile(getCachedFileContent(file), file);
  }

  private void undoChangesToFile(ChangedFile file) throws CoreException, IOException {
    String fileCache = getCachedFileContent(file);
    if (fileCache == null) {
      return;
    }
    if (file.isWorkspaceFile()) {
      applyChangesToFile(fileCache, file.getWorkspaceFile());
    } else {
      Files.writeString(file.getLocalPath(), fileCache, StandardCharsets.UTF_8);
    }
    removeCachedFileContent(file);
  }

  @Override
  public void onResolveAllChanges() {
    cleanupChangedFiles();
  }
}
