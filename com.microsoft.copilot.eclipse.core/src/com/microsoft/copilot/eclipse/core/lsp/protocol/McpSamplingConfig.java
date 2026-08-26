// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

/**
 * User preferences for MCP sampling requests from a server.
 */
public record McpSamplingConfig(boolean alwaysAllow, boolean alwaysDeny, List<String> allowedModels) {
}
