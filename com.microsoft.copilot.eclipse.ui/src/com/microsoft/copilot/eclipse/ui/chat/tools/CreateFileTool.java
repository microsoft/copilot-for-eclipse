package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

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
    return false;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

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
      result.addContent("Invalid file path: path cannot be empty");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    String content = (String) input.get("content");
    if (StringUtils.isBlank(content)) {
      result.addContent("Invalid input: content is required");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    try {
      // Resolve file in workspace
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IPath eclipsePath = Path.fromOSString(pathStr);
      IFile file = workspaceRoot.getFileForLocation(eclipsePath);

      if (file == null) {
        result.addContent("Invalid file path: " + pathStr + " does not exist in the workspace.");
        return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
      }

      // Check if file already exists
      if (file.exists()) {
        result.addContent("Failed: file already exists: " + pathStr + ". Please use edit file tool to update.");
        return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
      }

      // Create parent folders if needed
      createParentFolders(file.getParent());

      // Create file with content
      try (ByteArrayInputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
        file.create(contentStream, IResource.FORCE, new NullProgressMonitor());
      }
      file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

      // Open file in editor
      SwtUtils.invokeOnDisplayThread(() -> UiUtils.openInEditor(file));
      result.addContent("File created at: " + file.getFullPath().toOSString());
    } catch (CoreException e) {
      result.addContent("Error creating file: " + e.getMessage());
    } catch (IOException e) {
      result.addContent("Error handling file stream: " + e.getMessage());
    }

    return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
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
}
