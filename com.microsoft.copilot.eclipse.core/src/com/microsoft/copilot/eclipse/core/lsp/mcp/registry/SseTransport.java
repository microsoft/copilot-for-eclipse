// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

/**
 * Represents a Server-Sent Events (SSE) transport mechanism.
 */
public class SseTransport extends UrlBasedTransport {
  /**
   * Constructor for SseTransport.
   */
  public SseTransport() {
    super(TransportType.sse.name());
  }
}