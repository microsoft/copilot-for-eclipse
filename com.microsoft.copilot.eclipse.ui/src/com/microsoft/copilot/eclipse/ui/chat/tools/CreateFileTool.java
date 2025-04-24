package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Tool for creating files.
 */
public class CreateFileTool extends BaseTool {
  private static final String TOOL_NAME = "create_file";

  /**
   * Constructor for CreateFileTool.
   */
  public CreateFileTool() {
    this.name = TOOL_NAME;
  }

  @Override
  public boolean needConfirmation() {
    return true;
  }

  @Override
  public String getConfirmedMessage() {
    return "The tool is about to create a file to your workspace.\nDo you want to continue?";
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    // Create a new instance of LanguageModelToolInformation
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        This is a tool for creating a new file in the workspace.
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

    String pathStr = (String) input.get("filePath");
    if (StringUtils.isBlank(pathStr)) {
      result.addContent("Invalid file path");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    String content = (String) input.get("content");
    if (StringUtils.isBlank(content)) {
      result.addContent("Invalid input: content is required");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    Path path = Paths.get(pathStr);
    try {
      // Create file and its parent directories if they don't exist using NIO to avoid workspace issues for Eclipse
      Files.createDirectories(path.getParent());
      Files.write(path, content.getBytes());

      // Refresh the created file to make it visible for the Eclipse workspace
      org.eclipse.core.runtime.Path eclipsePath = new org.eclipse.core.runtime.Path(pathStr);
      IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(eclipsePath);
      file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
      SwtUtils.invokeOnDisplayThread(() -> UiUtils.openInEditor(file));

      result.addContent("File created at: " + pathStr);
    } catch (IOException | CoreException e) {
      result.addContent("Error creating file: " + e.getMessage());
    }
    return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
  }
}
