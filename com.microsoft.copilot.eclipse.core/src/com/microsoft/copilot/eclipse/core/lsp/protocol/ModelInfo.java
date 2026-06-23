// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Optional model metadata associated with a conversation turn. Mirrors the {@code modelInfo} field on the
 * {@code conversation/create} and {@code conversation/turn} LSP requests and responses.
 *
 * <p>The {@code id} and {@code providerName} fields are reserved for a future migration away from the legacy
 * {@code model} / {@code modelProviderName} fields and should not be relied upon today. The {@code reasoningEffort}
 * field carries the user-selected reasoning effort level (e.g. {@code low}, {@code medium}, {@code high}) when the
 * model surfaces selectable effort levels.
 *
 * @param id model identifier (optional)
 * @param providerName provider name (optional)
 * @param reasoningEffort user-selected reasoning effort (optional)
 * @param contextSize context size (optional)
 */
public record ModelInfo(String id, String providerName, String reasoningEffort, String contextSize) {
}
