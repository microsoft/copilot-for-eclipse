// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Optional model metadata associated with a conversation turn. Mirrors the {@code modelInfo} field on the
 * {@code conversation/create} and {@code conversation/turn} LSP requests and responses.
 *
 * <p>The language server resolves the turn's model from {@code id} (and {@code providerName} for BYOK models) in
 * preference to the legacy {@code model} / {@code modelProviderName} request fields, so {@code id} should carry the
 * concrete model id of the user-selected model. The {@code reasoningEffort} field carries the user-selected reasoning
 * effort level (e.g. {@code low}, {@code medium}, {@code high}) when the model surfaces selectable effort levels.
 *
 * @param id model identifier (optional)
 * @param providerName provider name (optional)
 * @param reasoningEffort user-selected reasoning effort (optional)
 * @param contextSize user-selected context-window size in tokens (optional). Sent as a JSON number to match the
 *     language server's {@code modelInfo.contextSize} schema.
 */
public record ModelInfo(String id, String providerName, String reasoningEffort, Integer contextSize) {
}
