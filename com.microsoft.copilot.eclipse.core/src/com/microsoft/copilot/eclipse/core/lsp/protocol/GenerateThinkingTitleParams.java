// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the {@code thinking/generateTitle} request.
 *
 * <p>Either {@code thinkingContent} or {@code extractedTitles} should be provided depending on
 * whether parsed section titles are available on the client side.
 *
 * @param thinkingContent the raw thinking content (used when no extracted titles are available)
 * @param extractedTitles previously extracted section titles, may be {@code null}
 */
public record GenerateThinkingTitleParams(String thinkingContent, String[] extractedTitles) {
}
