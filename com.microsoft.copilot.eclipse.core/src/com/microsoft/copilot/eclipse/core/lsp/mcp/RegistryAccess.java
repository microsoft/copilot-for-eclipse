// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp;

/**
 * Defines the access level granted for an MCP registry.
 */
public enum RegistryAccess {
  /**
   * Access limited to registry operations only.
   */
  registry_only,

  /**
   * Full access to all registry features.
   */
  allow_all
}
