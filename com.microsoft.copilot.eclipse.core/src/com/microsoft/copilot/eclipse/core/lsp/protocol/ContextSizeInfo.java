// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Context size information sent by the language server during conversation progress. Contains token usage breakdown
 * across different categories such as system prompts, tool definitions, user messages, and tool results.
 */
public record ContextSizeInfo(
    int totalTokenLimit,
    int systemPromptTokens,
    int toolDefinitionTokens,
    int userMessagesTokens,
    int assistantMessagesTokens,
    int attachedFilesTokens,
    int toolResultsTokens,
    int totalUsedTokens,
    double utilizationPercentage) {
}
