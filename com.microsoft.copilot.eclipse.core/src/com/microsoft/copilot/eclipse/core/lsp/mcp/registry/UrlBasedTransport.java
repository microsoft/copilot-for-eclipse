package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

/**
 * Abstract base class representing a URL-based transport mechanism.
 */
public abstract class UrlBasedTransport extends Transport {
  private String url;
  private List<KeyValueInput> headers;

  /**
   * Constructor for UrlBasedTransport.
   *
   * @param type The type of transport.
   */
  protected UrlBasedTransport(String type) {
    super(type);
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
}
