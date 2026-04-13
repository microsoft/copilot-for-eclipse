// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.resources.IFile;
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
    CompletableFuture<LanguageModelToolResult[]> resultFuture = new CompletableFuture<>();
    if (input.get("filePath") instanceof String filePath) {
      IFile file = FileUtils.getFileFromPath(filePath, true);

      if (file == null || !file.exists()) {
        resultFuture.complete(new LanguageModelToolResult[] {
            new LanguageModelToolResult("The file path provided does not exist. Please check the path and try again.",
                ToolInvocationStatus.error) });
        return resultFuture;
      }

      if (input.get("code") instanceof String code) {
        CopilotUi.getPlugin().getChatServiceManager().getFileToolService().addChangedFile(file, FileChangeType.Changed);
        cacheTheOriginalFileContent(file);
        try {
          applyChangesToFile(code, file);
        } catch (CoreException | IOException e) {
          CopilotCore.LOGGER.error("Error replacing file content", e);
          resultFuture.complete(new LanguageModelToolResult[] { new LanguageModelToolResult(
              "Failed to apply changes to the file: " + e.getMessage(), ToolInvocationStatus.error) });
          return resultFuture;
        }
        refreshCompareEditorIfOpen(fileContentCache.get(file), file);
        // Must return the updated content as a result to the CLS.
        resultFuture.complete(
            new LanguageModelToolResult[] { new LanguageModelToolResult(code, ToolInvocationStatus.success) });
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

  private void applyChangesToFile(String changedContent, IFile file) throws CoreException, IOException {
    if (!validateEdit(file)) {
      throw new IllegalStateException("File validation failed for " + file.getFullPath());
    }
    ByteArrayInputStream inputStream = getInputStream(changedContent, file);

    // Set the file contents
    file.setContents(inputStream, true, true, new NullProgressMonitor());

    // Refresh the file to ensure Eclipse recognizes the changes
    file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

    // Close the input stream
    inputStream.close();
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
  public void onKeepChange(IFile file) {
    fileContentCache.remove(file);
    closeCompareEditor(file);
  }

  @Override
  public void onKeepAllChanges(List<IFile> files) {
    for (IFile file : files) {
      onKeepChange(file);
    }
  }

  @Override
  public void onUndoChange(IFile file) throws CoreException, IOException {
    undoChangesToFile(file);
    closeCompareEditor(file);
  }

  @Override
  public void onUndoAllChanges(List<IFile> files) throws CoreException, IOException {
    for (IFile file : files) {
      onUndoChange(file);
    }
  }

  @Override
  public void onViewDiff(IFile file) {
    CompareEditorInput input = compareEditorInputMap.get(file);
    if (input != null) {
      if (isCompareEditorOpen(input)) {
        bringCompareEditorToTop(input);
        return;
      }
      // Compare editor was closed by the user, remove stale entry and recreate
      compareEditorInputMap.remove(file);
    }
    compareStringWithFile(fileContentCache.get(file), file);
  }

  @Override
  public void onResolveAllChanges() {
    cleanupChangedFiles();
  }

  private void undoChangesToFile(IFile file) throws CoreException, IOException {
    String fileCache = fileContentCache.get(file);
    if (fileCache != null) {
      applyChangesToFile(fileCache, file);
    }
    fileContentCache.remove(file);
  }
}
