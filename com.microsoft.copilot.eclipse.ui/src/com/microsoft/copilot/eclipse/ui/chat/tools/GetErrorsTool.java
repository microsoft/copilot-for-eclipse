package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.lsp4e.LSPEclipseUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Tool to get errors from the Problems view.
 */
public class GetErrorsTool extends BaseTool {
  private static final String TOOL_NAME = "get_errors";

  /**
   * Constructor for the GetErrorsTool.
   */
  public GetErrorsTool() {
    this.name = TOOL_NAME;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    // Create a new instance of LanguageModelToolInformation
    LanguageModelToolInformation toolInfo = new LanguageModelToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription(
        """
            Get any compile or lint errors in a code file.
            If the user mentions errors or problems in a file, they may be referring to these.
            Use the tool to see the same errors that the user is seeing.
            Also use this tool after editing a file to validate the change.
            """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put("filePaths", new InputSchemaPropertyValue("array", ""));

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(Arrays.asList("filePaths"));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    LanguageModelToolResult toolResult = new LanguageModelToolResult();
    List<String> fileUris = validateInput(input.get("filePaths"));
    if (fileUris == null) {
      toolResult.addContent("The value of filePaths is not in the type of string array.");
    } else if (fileUris.isEmpty()) {
      toolResult.addContent("The tool cannot be invoked because input is empty.");
    } else {
      String errors = getErrors(fileUris);
      toolResult.addContent(errors);
    }

    return CompletableFuture.completedFuture(new LanguageModelToolResult[] { toolResult });
  }

  /**
   * Validates the input for the tool.
   *
   * @param filePathsObj The input object to validate.
   * @return An List of Strings if valid, null otherwise.
   */
  public List<String> validateInput(Object filePathsObj) {
    if (filePathsObj instanceof List<?> filePathsList) {
      if (filePathsList.stream().allMatch(String.class::isInstance)) {
        return (List<String>) filePathsList;
      }
    } else if (filePathsObj instanceof String filePathsStr) {
      Gson gson = new Gson();
      try {
        List<?> tempList = gson.fromJson(filePathsStr, List.class);
        if (tempList.stream().allMatch(String.class::isInstance)) {
          return (List<String>) tempList;
        }
      } catch (JsonSyntaxException e) {
        return null;
      }
    }

    return null;
  }

  /**
   * Retrieves errors from the Problems view for the given file URIs.
   *
   * @param fileUris The list of file URIs to check for errors.
   * @return A string containing the errors found.
   */
  public String getErrors(List<String> fileUris) {
    StringBuilder toolResult = new StringBuilder();

    for (String fileUri : fileUris) {
      fileUri = String.valueOf(resolveFilePath(fileUri));
      try {
        IResource resource = LSPEclipseUtils.findResourceFor(fileUri);
        if (resource == null) {
          toolResult.append("Resource not found for fileUri: ").append(fileUri).append(StringUtils.LF);
          continue;
        }

        IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
        for (IMarker marker : markers) {
          if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
            toolResult.append(marker.toString()).append(StringUtils.LF);
          }
        }
      } catch (CoreException e) {
        toolResult.append("Failed on file: ").append(fileUri).append(e.getMessage()).append(StringUtils.LF);
      }
    }

    return toolResult.toString();
  }

  /**
   * Resolves the file path to a URI. Public only for testing purpose
   *
   * @param filepath The file path to resolve.
   * @return The resolved URI, or null if the path is invalid.
   */
  public URI resolveFilePath(String filepath) {
    // Check for posix-like absolute paths or Windows-like absolute paths
    if (filepath.startsWith("/")
        || (PlatformUtils.isWindows() && (hasDriveLetter(filepath) || filepath.startsWith("\\")))) {
      return Paths.get(filepath).toUri();
    }

    // Check if the filepath starts with a scheme
    if (Pattern.compile("\\w[\\w\\d+.-]*:\\S").matcher(filepath).find()) {
      try {
        return new URI(filepath);
      } catch (URISyntaxException e) {
        return null;
      }
    }

    return null;
  }

  private boolean hasDriveLetter(String filepath) {
    return filepath.length() > 1 && Character.isLetter(filepath.charAt(0)) && filepath.charAt(1) == ':';
  }
}
