package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameters for listing servers from the MCP Registry.
 */
public class ListServersParams {
  private String baseUrl; // {baseApiUrl}/{apiVersion}/servers
  private String cursor;
  private int limit; // minimum: 1
  private String search;
  private String updatedSince;
  private String version;

  /**
   * Constructor for ListServersParams without updatedSince.
   *
   * @param baseUrl The base URL for the list server API.
   * @param cursor  The cursor for pagination.
   * @param limit   The maximum number of results to return.
   * @param search  The search query.
   * @param version The API version.
   */
  public ListServersParams(String baseUrl, String cursor, int limit, String search, String version) {
    this(baseUrl, cursor, limit, search, null, version);
  }

  /**
   * Constructor for ListServersParams.
   *
   * @param baseUrl      The base URL for the list server API.
   * @param cursor       The cursor for pagination.
   * @param limit        The maximum number of results to return.
   * @param search       The search query.
   * @param updatedSince The timestamp to filter servers updated since.
   * @param version      The API version.
   */
  public ListServersParams(String baseUrl, String cursor, int limit, String search, String updatedSince,
      String version) {
    this.baseUrl = baseUrl;
    this.cursor = cursor;
    this.limit = limit;
    this.search = search;
    this.updatedSince = updatedSince;
    this.version = version;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getCursor() {
    return cursor;
  }

  public void setCursor(String cursor) {
    this.cursor = cursor;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public String getUpdatedSince() {
    return updatedSince;
  }

  public void setUpdatedSince(String updatedSince) {
    this.updatedSince = updatedSince;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, cursor, limit, search, updatedSince, version);
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
        && Objects.equals(limit, other.limit) && Objects.equals(search, other.search)
        && Objects.equals(updatedSince, other.updatedSince) && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("baseUrl", baseUrl);
    builder.add("cursor", cursor);
    builder.add("limit", limit);
    builder.add("search", search);
    builder.add("updatedSince", updatedSince);
    builder.add("version", version);
    return builder.toString();
  }

}
