// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.lsp4j.FileChangeType;

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
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Tool for creating files.
 */
public class CreateFileTool extends FileToolBase implements WorkingSetHandler {
  public static final String TOOL_NAME = "create_file";

  /**
   * Constructor for CreateFileTool.
   */
  public CreateFileTool() {
    super();
    this.name = TOOL_NAME;
  }

  @Override
  public boolean needConfirmation() {
    return false;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        This is a tool for creating a new workspace file or a new file at an absolute local filesystem path.
        The file will be created with the specified content.
        """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put("filePath", new InputSchemaPropertyValue("string", "The absolute path to the file to create."));
    properties.put("content", new InputSchemaPropertyValue("string", "The content to write to the file."));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(Arrays.asList("filePath", "content"));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);
    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    LanguageModelToolResult result = new LanguageModelToolResult();

    String filePath = (String) input.get("filePath");
    if (StringUtils.isBlank(filePath)) {
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("Invalid file path: path cannot be empty");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    String content = StringUtils.isBlank((String) input.get("content")) ? "" : (String) input.get("content");
    result = createFile(filePath, content);

    return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
  }

  private LanguageModelToolResult createFile(String filePath, String content) {
    IFile file = FileUtils.getFileFromPath(filePath, false);

    if (file != null && file.getProject().exists()) {
      return createWorkspaceFile(file, filePath, content);
    }

    Path localPath = getLocalFilePath(filePath);
    if (localPath != null) {
      return createLocalFile(localPath, content);
    }

    LanguageModelToolResult result = new LanguageModelToolResult();
    result.setStatus(ToolInvocationStatus.error);
    result.addContent("Invalid file path: " + filePath + " does not exist in the workspace.");
    return result;
  }

  private LanguageModelToolResult createWorkspaceFile(IFile file, String filePath, String content) {
    LanguageModelToolResult result = new LanguageModelToolResult();

    try {
      if (file.exists()) {
        result.setStatus(ToolInvocationStatus.error);
        result.addContent("Failed: file already exists: " + filePath + ". Please use edit file tool to update.");
        return result;
      }

      createParentFolders(file.getParent());

      try (ByteArrayInputStream contentStream = new ByteArrayInputStream(
          content.getBytes(PlatformUtils.getFileCharset(file)))) {
        file.create(contentStream, IResource.FORCE, new NullProgressMonitor());
        cacheTheOriginalFileContent(ChangedFile.workspace(file), StringUtils.EMPTY);
      }
      CopilotUi.getPlugin().getChatServiceManager().getFileToolService().addChangedFile(ChangedFile.workspace(file),
          FileChangeType.Created);
      file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

      result.addContent("File created at: " + file.getFullPath().toOSString());
      result.setStatus(ToolInvocationStatus.success);
    } catch (CoreException e) {
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("Error creating file: " + e.getMessage());
    } catch (IOException e) {
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("Error handling file stream: " + e.getMessage());
    }

    return result;
  }

  private LanguageModelToolResult createLocalFile(Path filePath, String content) {
    LanguageModelToolResult result = new LanguageModelToolResult();
    Path normalizedPath = normalizeLocalPath(filePath);
    if (Files.exists(normalizedPath, LinkOption.NOFOLLOW_LINKS)) {
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("Failed: file already exists: " + normalizedPath + ". Please use edit file tool to update.");
      return result;
    }

    try {
      Path parent = normalizedPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(normalizedPath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      cacheTheOriginalFileContent(ChangedFile.local(normalizedPath), StringUtils.EMPTY);
      CopilotUi.getPlugin().getChatServiceManager().getFileToolService().addChangedFile(
          ChangedFile.local(normalizedPath), FileChangeType.Created);
      result.addContent("File created at: " + normalizedPath);
      result.setStatus(ToolInvocationStatus.success);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Error creating local file", e);
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("Error creating file: " + e.getMessage());
    }

    return result;
  }

  /**
   * Creates parent folders if they don't exist.
   *
   * @param parent The parent resource
   * @throws CoreException If there's an error creating the folders
   */
  private void createParentFolders(IResource parent) throws CoreException {
    if (parent == null || parent.exists()) {
      return;
    }

    createParentFolders(parent.getParent());

    if (parent instanceof IFolder) {
      ((IFolder) parent).create(IResource.FORCE, true, new NullProgressMonitor());
    }
  }

  @Override
  public void onKeepChange(ChangedFile file) {
    removeCachedFileContent(file);
    closeCompareEditor(file);
  }

  @Override
  public void onUndoChange(ChangedFile file) throws CoreException, IOException {
    deleteCreatedFile(file);
    removeCachedFileContent(file);
    closeCompareEditor(file);
  }

  private void deleteCreatedFile(ChangedFile file) throws CoreException, IOException {
    if (file.isWorkspaceFile()) {
      IFile workspaceFile = file.getWorkspaceFile();
      if (workspaceFile != null && workspaceFile.exists()) {
        workspaceFile.delete(true, new NullProgressMonitor());
      }
      return;
    }
    Files.deleteIfExists(file.getLocalPath());
  }

  @Override
  public void onViewDiff(ChangedFile file) {
    if (file.isWorkspaceFile()) {
      SwtUtils.invokeOnDisplayThreadAsync(() -> UiUtils.openInEditor(file.getWorkspaceFile()));
      return;
    }
    SwtUtils.invokeOnDisplayThreadAsync(() -> UiUtils.openLocalFileInEditor(file.getLocalPath()));
  }

  @Override
  public void onResolveAllChanges() {
    cleanupChangedFiles();
  }
}
