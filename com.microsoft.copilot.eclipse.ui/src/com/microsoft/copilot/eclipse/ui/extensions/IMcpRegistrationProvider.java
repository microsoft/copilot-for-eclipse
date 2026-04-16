// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.extensions;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for providing MCP (Model Context Protocol) server configurations.
 * Implementations of this interface can be contributed via the mcpRegistration extension point
 * to dynamically provide MCP server configurations to the GitHub Copilot plugin.
 */
public interface IMcpRegistrationProvider {

  /**
   * Provides MCP server configurations in JSON format asynchronously.
   *
   * @return CompletableFuture containing JSON string with MCP server configurations in the format:
   *         {"servers":{"serverName1":{...config...},"serverName2":{...config...}}}
   *         Returns null or empty string if no configurations are available.
   */
  CompletableFuture<String> getMcpServerConfigurations();
}
