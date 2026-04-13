// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

/**
 * Represents a standard input/output (stdio) transport mechanism.
 */
public class StdioTransport extends Transport {

  /**
   * Constructor for StdioTransport.
   */
  public StdioTransport() {
    super(TransportType.stdio.name());
  }
}
