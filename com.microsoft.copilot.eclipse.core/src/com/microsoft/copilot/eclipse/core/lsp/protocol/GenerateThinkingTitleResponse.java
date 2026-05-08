// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Response for the {@code thinking/generateTitle} request.
 *
 * @param title the title returned by the language server
 */
public record GenerateThinkingTitleResponse(String title) {
}
