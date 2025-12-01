package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

/**
 * Represents a streamable HTTP transport mechanism.
 */
public class StreamableHttpTransport extends UrlBasedTransport {
  /**
   * Constructor for StreamableHttpTransport.
   */
  public StreamableHttpTransport() {
    super(TransportType.streamable_http.name());
  }
}
