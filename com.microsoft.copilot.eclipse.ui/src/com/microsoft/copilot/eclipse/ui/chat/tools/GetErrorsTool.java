// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.core.lsp.protocol.tools.GetErrorsResult;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Tool to get errors from the Problems view.
 */
public class GetErrorsTool extends BaseTool {
  private static final String TOOL_NAME = "get_errors";
  private static final String FILE_PATHS = "filePaths";

  /**
   * Constructor for the GetErrorsTool.
   */
  public GetErrorsTool() {
    this.name = TOOL_NAME;
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();

    // Set the name and description of the tool
    toolInfo.setName(TOOL_NAME);
    toolInfo.setDescription("""
        Get any compile or lint errors in a code file.
        If the user mentions errors or problems in a file, they may be referring to these.
        Use the tool to see the same errors that the user is seeing.
        Also use this tool after editing a file to validate the change.
        """);

    // Define the input schema for the tool
    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    // Define the properties of the input schema
    InputSchemaPropertyValue filePathsProperty = new InputSchemaPropertyValue("array",
        "Array of absolute file paths to check for errors");
    filePathsProperty.setItems(new InputSchemaPropertyValue("string",
        "The absolute path of a file to check for compile/lint errors"));
    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    properties.put(FILE_PATHS, filePathsProperty);

    // Set the properties and required fields for the input schema
    inputSchema.setProperties(properties);
    inputSchema.setRequired(List.of(FILE_PATHS));

    // Attach the input schema to the tool information
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    LanguageModelToolResult toolResult = new LanguageModelToolResult();
    List<String> filePaths = validateInput(input.get(FILE_PATHS));
    if (filePaths == null) {
      toolResult.setStatus(ToolInvocationStatus.error);
      toolResult.addContent("The value of filePaths is not in the type of string array.");
    } else if (filePaths.isEmpty()) {
      toolResult.setStatus(ToolInvocationStatus.error);
      toolResult.addContent("The tool cannot be invoked because input is empty.");
    } else {
      GetErrorsResult result = getErrors(filePaths);
      toolResult.addContent(result.content());
      toolResult.setStatus(result.hasException() ? ToolInvocationStatus.error : ToolInvocationStatus.success);
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
    } else if (filePathsObj instanceof String filePaths) {
      Gson gson = new Gson();
      try {
        List<?> tempList = gson.fromJson(filePaths, List.class);
        if (tempList.stream().allMatch(String.class::isInstance)) {
          return (List<String>) tempList;
        }
      } catch (JsonSyntaxException e) {
        // Try lenient parsing for malformed JSON with invalid escape sequences
        // (e.g., when model sends "[\"\\path\\file\"]" which after JSON parsing
        // becomes "[\"\path\file\"]" with invalid \p and \f escapes)
        return parseMalformedJsonArray(filePaths);
      }
    }

    return null;
  }

  /**
   * Attempts to parse a malformed JSON array string that may contain invalid escape sequences. This handles cases where
   * the model sends a JSON string with paths containing backslashes that weren't properly double-escaped.
   *
   * @param jsonStr The potentially malformed JSON array string.
   * @return A list of extracted strings, or null if parsing fails.
   */
  private List<String> parseMalformedJsonArray(String jsonStr) {
    if (jsonStr == null || !jsonStr.startsWith("[") || !jsonStr.endsWith("]")) {
      return null;
    }

    // Remove the outer brackets and trim
    String content = jsonStr.substring(1, jsonStr.length() - 1).trim();
    if (content.isEmpty()) {
      return List.of();
    }

    // Use regex to extract quoted strings, handling escaped quotes within
    List<String> result = new ArrayList<>();
    Matcher matcher = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(content);
    while (matcher.find()) {
      String value = matcher.group(1);
      try {
        value = new Gson().fromJson("\"" + value + "\"", String.class);
      } catch (JsonSyntaxException e) {
        value = value.replace("\\\"", "\"").replace("\\\\", "\\");
      }
      result.add(value);
    }

    return result.isEmpty() ? null : result;
  }

  /**
   * Retrieves errors from the Problems view for the given file URIs.
   *
   * @param filePaths The list of file paths to check for errors.
   * @return A GetErrorsResult containing the errors found and whether any exceptions occurred.
   */
  public GetErrorsResult getErrors(List<String> filePaths) {
    StringBuilder toolResult = new StringBuilder();
    boolean hasException = false;

    for (String filePath : filePaths) {
      try {
        IFile file = FileUtils.getFileFromPath(filePath, true);

        if (file == null || !file.exists()) {
          toolResult.append("Resource not found for filePath: ").append(filePath).append(StringUtils.LF);
          continue;
        }

        IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
        for (IMarker marker : markers) {
          if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
            toolResult.append(marker.toString()).append(StringUtils.LF);
          }
        }
      } catch (CoreException e) {
        toolResult.append("Failed on file: ").append(filePath).append(e.getMessage()).append(StringUtils.LF);
        hasException = true;
      }
    }

    return new GetErrorsResult(toolResult.toString(), hasException);
  }

}