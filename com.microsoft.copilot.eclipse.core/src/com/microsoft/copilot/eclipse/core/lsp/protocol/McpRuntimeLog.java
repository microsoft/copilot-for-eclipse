package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a runtime log for the MCP service.
 */
public class McpRuntimeLog {

  private McpRuntimeLogLevel level;
  private String server;
  private String tool;
  private String message;
  private Long time;

  public McpRuntimeLogLevel getLevel() {
    return level;
  }

  public void setLevel(McpRuntimeLogLevel level) {
    this.level = level;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getTool() {
    return tool;
  }

  public void setTool(String tool) {
    this.tool = tool;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Long getTime() {
    return time;
  }

  public void setTime(Long time) {
    this.time = time;
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, server, tool, message, time);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    McpRuntimeLog other = (McpRuntimeLog) obj;
    return level == other.level && Objects.equals(server, other.server) && Objects.equals(tool, other.tool)
        && Objects.equals(message, other.message) && time == other.time;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("level", level);
    builder.add("server", server);
    builder.add("tool", tool);
    builder.add("message", message);
    builder.add("time", LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()));
    return builder.toString();
  }

  /**
   * Enum representing different levels of runtime logs.
   */
  enum McpRuntimeLogLevel {
    info, warning, error
  }
}