// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Wire-level "thinking" payload streamed from the language server inside ChatProgressValue.
 *
 * <p>Each progress report may carry a delta with new text content. The UI is responsible for
 * accumulating the deltas across reports and rendering the aggregated content.
 *
 * @param id the (optional) identifier of the thinking block
 * @param text the delta text for this report; callers should treat blank text as "no content"
 * @param encrypted the (optional) encrypted form of the thinking content
 */
public record Thinking(String id, String text, String encrypted) {
}
