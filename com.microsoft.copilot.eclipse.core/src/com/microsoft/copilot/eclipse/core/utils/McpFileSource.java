package com.microsoft.copilot.eclipse.core.utils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a single discovered mcp.json file and its parsed servers.
 *
 * @param label human-readable label 
 * @param filePath absolute path to the mcp.json file
 * @param servers parsed server map from the file
 */
public record McpFileSource(String label, Path filePath, Map<String, Object> servers) {

  /**
   * Creates a new MCP file source
   *
   * @param label human-readable label 
   * @param filePath absolute path to the mcp.json file
   * @param servers parsed server map from the file
   */
  public McpFileSource {
    // normalize null servers to an empty unmodifiable map
    servers = servers != null ? Collections.unmodifiableMap(servers) : Collections.emptyMap();
  }

  /**
   * @return human-readable label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return absolute path to the mcp.json file
   */
  public Path getFilePath() {
    return filePath;
  }

  /**
   * @return unmodifiable map of parsed servers
   */
  public Map<String, Object> getServers() {
    return servers;
  }

  /**
   * @return number of servers in this source 
   */
  public int getServerCount() {
    return servers.size();
  }
}
