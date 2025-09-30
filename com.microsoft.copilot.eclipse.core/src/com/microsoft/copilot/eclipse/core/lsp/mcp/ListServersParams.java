package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for listing servers from the MCP Registry.
 */
public class ListServersParams {
  private String baseUrl;
  private int limit;
  private String cursor;

  /**
   * Constructor for McpRegistryListServersParams.
   *
   * @param baseUrl The base URL of the MCP Registry API.
   * @param limit The maximum number of servers to return.
   * @param cursor The pagination cursor (offset) for the server list.
   */
  public ListServersParams(String baseUrl, int limit, String cursor) {
    this.baseUrl = baseUrl;
    this.limit = limit;
    this.cursor = cursor;
  }

  /**
   * Constructor for McpRegistryListServersParams with default limit and offset.
   *
   * @param baseUrl The base URL of the MCP Registry API.
   */
  public ListServersParams(String baseUrl) {
    this(baseUrl, 1, "");
  }

  /**
   * Constructor for McpRegistryListServersParams with default offset.
   *
   * @param baseUrl The base URL of the MCP Registry API.
   * @param limit The maximum number of servers to return.
   */
  public ListServersParams(String baseUrl, int limit) {
    this(baseUrl, limit, "");
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, cursor, limit);
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
    ListServersParams other = (ListServersParams) obj;
    return Objects.equals(baseUrl, other.baseUrl) && Objects.equals(cursor, other.cursor)
        && Objects.equals(limit, other.limit);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("baseUrl", baseUrl);
    builder.append("limit", limit);
    builder.append("cursor", cursor);
    return builder.toString();
  }

}
