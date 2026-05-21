// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the {@code $/copilot/compressionCompleted} notification sent by the language server when automatic
 * conversation compression finishes. The {@code contextInfo} field is optional and may be {@code null}.
 */
public record CompressionCompletedParams(
    String conversationId,
    int archivedPartitionId,
    int newPartitionId,
    int summaryLength,
    int turnCount,
    int durationMs,
    ContextSizeInfo contextInfo) {
}
