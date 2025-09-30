package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Information about a remote server configuration in the MCP Registry.
 */
public class Remote {
  @SerializedName("type")
  private String transportType;
  private String url;
  private List<KeyValueInput> headers;

  /**
   * Constructor for Remote.
   *
   * @param transportType The transport type (e.g., STREAMABLE, SSE).
   * @param url The URL of the remote server.
   * @param headers The headers to include in requests to the remote server.
   */
  public Remote(String transportType, String url, List<KeyValueInput> headers) {
    this.transportType = transportType;
    this.url = url;
    this.headers = headers;
  }

  public String getTransportType() {
    return transportType;
  }

  public void setTransportType(String transportType) {
    this.transportType = transportType;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<KeyValueInput> getHeaders() {
    return headers;
  }

  public void setHeaders(List<KeyValueInput> headers) {
    this.headers = headers;
  }

  @Override
  public int hashCode() {
    return Objects.hash(headers, transportType, url);
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
    Remote other = (Remote) obj;
    return Objects.equals(headers, other.headers) && transportType == other.transportType
        && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("transportType", transportType);
    builder.append("url", url);
    builder.append("headers", headers);
    return builder.toString();
  }

  /**
   * Enum representing the transport type for the remote server.
   */
  public static enum TransportType {
    streamable, streamable_http, sse;
  }
}
