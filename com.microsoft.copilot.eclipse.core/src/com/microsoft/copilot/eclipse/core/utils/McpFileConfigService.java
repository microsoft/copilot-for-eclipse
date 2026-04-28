// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Service for discovering and reading MCP server configuration from mcp.json files.
 *
 * <p>Supports three levels of configuration (lowest to highest priority):
 * <ol>
 *   <li>Global: {@code ~/.config/github-copilot/eclipse/mcp.json} (Linux/macOS)
 *       or {@code %APPDATA%\github-copilot\eclipse\mcp.json} (Windows)</li>
 *   <li>Project: {@code .vscode/mcp.json} (cross-IDE, per project root)</li>
 *   <li>Project: {@code .github/copilot/mcp.json} (Copilot-specific, per project root)</li>
 * </ol>
 *
 * <p>Servers from all discovered files are merged, with higher-priority sources overriding
 * lower-priority ones for servers with the same name.
 */
public final class McpFileConfigService {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  private McpFileConfigService() {
    // Utility class, not instantiable
  }

  /**
   * Resolves the global MCP config file path for the current platform.
   *
   * @return the global mcp.json path, or null if the home directory cannot be determined
   */
  public static Path resolveGlobalConfigPath() {
    if (PlatformUtils.isWindows()) {
      String appData = System.getenv("APPDATA");
      if (StringUtils.isNotBlank(appData)) {
        return Paths.get(appData, "github-copilot", Constants.MCP_FILE_GLOBAL_SUBDIR, Constants.MCP_FILE_NAME);
      }
    } else {
      String home = System.getProperty("user.home");
      if (StringUtils.isNotBlank(home)) {
        return Paths.get(home, ".config", "github-copilot", Constants.MCP_FILE_GLOBAL_SUBDIR, Constants.MCP_FILE_NAME);
      }
    }
    return null;
  }

  /**
   * Discovers all mcp.json files and returns them as a list of sources, ordered from lowest to highest priority.
   *
   * @param projectRoots list of absolute project root paths in the workspace
   * @return ordered list of discovered file sources (global first, then projects)
   */
  public static List<McpFileSource> discoverMcpJsonFiles(List<Path> projectRoots) {
    List<McpFileSource> sources = new ArrayList<>();

    // 1. Global config (lowest priority)
    Path globalPath = resolveGlobalConfigPath();
    if (globalPath != null) {
      Map<String, Object> servers = readServersFromFile(globalPath);
      if (servers != null && !servers.isEmpty()) {
        sources.add(new McpFileSource("Global", globalPath, servers));
      }
    }

    // 2. Project-level configs
    if (projectRoots != null) {
      for (Path projectRoot : projectRoots) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
          continue;
        }

        // .vscode/mcp.json (cross-IDE, lower project priority)
        addMcpFileBasedServers(sources, projectRoot, projectRoot.resolve(Constants.MCP_FILE_VSCODE), ".vscode");
        // .github/copilot/mcp.json (Copilot-specific, higher project priority)
        addMcpFileBasedServers(sources, projectRoot, projectRoot.resolve(Constants.MCP_FILE_COPILOT), ".github/copilot");
      }
    }

    return sources;
  }

  private static void addMcpFileBasedServers(List<McpFileSource> sources, Path projectRoot, Path mcpFilePath, String labelSuffix) {
    Map<String, Object> vscodeServers = readServersFromFile(mcpFilePath);
    if (vscodeServers != null && !vscodeServers.isEmpty()) {
      String projectName = projectRoot.getFileName().toString();
      sources.add(new McpFileSource(projectName + " ( " + labelSuffix + " )", mcpFilePath, vscodeServers));
    }
  }

  /**
   * Merges all discovered file-based MCP server configs into a single JSON string.
   *
   * @param sources ordered list of file sources to merge
   * @return merged JSON string, or empty string if no servers found
   */
  public static String mergeMcpFilesToSingleJson(List<McpFileSource> sources) {
    if (sources == null || sources.isEmpty()) {
      return "";
    }

    Map<String, Object> merged = new LinkedHashMap<>();
    for (McpFileSource source : sources) {
      // merged in priority order, so later sources override earlier ones for duplicate server names
      merged.putAll(source.getServers());
    }

    if (merged.isEmpty()) {
      return "";
    }

    Map<String, Object> wrapper = new LinkedHashMap<>();
    wrapper.put("servers", merged);
    return GSON.toJson(wrapper);
  }

  /**
   * Discovers all file sources from the given project roots, merges them, and returns the JSON string.
   *
   * @param projectRoots list of absolute project root paths
   * @return merged JSON string of all file-based servers, or empty string
   */
  public static String loadAndMerge(List<Path> projectRoots) {
    List<McpFileSource> sources = discoverMcpJsonFiles(projectRoots);
    return mergeMcpFilesToSingleJson(sources);
  }

  /**
   * Reads and parses the "servers" object from an mcp.json file.
   *
   * <p>Accepts two formats:
   * <ul>
   *   <li>{@code {"servers": {"name": {...}, ...}}} — extracts the "servers" value</li>
   *   <li>{@code {"name": {...}, ...}} — treats the entire object as servers</li>
   * </ul>
   *
   * @param filePath path to the mcp.json file
   * @return map of server-name → server-config, or null if file doesn't exist or is invalid
   */
  @SuppressWarnings("unchecked")
  static Map<String, Object> readServersFromFile(Path filePath) {
    if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
      return null;
    }

    // Security: verify the file path is canonical (prevent path traversal)
    if (!isCanonicalPath(filePath)) {
      return null;
    }

    String content;
    try {
      content = Files.readString(filePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to read mcp.json: " + filePath, e);
      return null;
    }

    if (StringUtils.isBlank(content)) {
      return null;
    }

    return parseServers(content, filePath.toString());
  }

  private static boolean isCanonicalPath(Path filePath) {
    try {
      Path canonical = filePath.toRealPath();
      if (!canonical.equals(filePath.toAbsolutePath().normalize())) {
        CopilotCore.LOGGER.info("Rejected mcp.json with non-canonical path: " + filePath);
        return false;
      }
      return true;
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to resolve canonical path for: " + filePath, e);
      return false;
    }
  }

  /**
   * Parses the "servers" from an mcp.json content string.
   *
   * @param mcpJsonContent the JSON content
   * @param sourceLabel label for error messages (typically the file path)
   * @return map of server-name → server-config, or null on parse failure
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parseServers(String mcpJsonContent, String sourceLabel) {
    try {
      Map<String, Object> jsonMap = GSON.fromJson(mcpJsonContent, new TypeToken<Map<String, Object>>() {
      }.getType());

      if (jsonMap == null) {
        return null;
      }

      // If the top-level has a "servers" key, extract that
      if (jsonMap.containsKey("servers")) {
        Object serversObj = jsonMap.get("servers");
        if (serversObj instanceof Map) {
          return (Map<String, Object>) serversObj;
        }
        CopilotCore.LOGGER.info("Invalid 'servers' value in " + sourceLabel + ": expected object");
        return null;
      }

      // Otherwise treat the entire object as the servers map (VS Code compat)
      // But filter out known non-server keys like "inputs"
      Map<String, Object> servers = new LinkedHashMap<>(jsonMap);
      servers.remove("inputs");
      return servers.isEmpty() ? null : servers;
    } catch (JsonParseException e) {
      CopilotCore.LOGGER.error("Failed to parse mcp.json: " + sourceLabel, e);
      return null;
    }
  }
}
