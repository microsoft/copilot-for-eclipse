package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for getting a specific server from the MCP Registry.
 */
public class GetServerParams {
  private String baseUrl;
  private String id;
  private String version;

  /**
   * Constructor for McpRegistryGetServerParams.
   *
   * @param baseUrl The base URL of the MCP Registry API.
   * @param id The unique identifier of the server.
   * @param version The version of the server.
   */
  public GetServerParams(String baseUrl, String id, String version) {
    this.baseUrl = baseUrl;
    this.id = id;
    this.version = version;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, id, version);
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
    GetServerParams other = (GetServerParams) obj;
    return Objects.equals(baseUrl, other.baseUrl) && Objects.equals(id, other.id)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("baseUrl", baseUrl);
    builder.append("id", id);
    builder.append("version", version);
    return builder.toString();
  }

}
