// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the {@code $/copilot/compressionStarted} notification sent by the language server when automatic
 * conversation compression begins.
 */
public record CompressionStartedParams(String conversationId, int partitionId, String reason) {
}
