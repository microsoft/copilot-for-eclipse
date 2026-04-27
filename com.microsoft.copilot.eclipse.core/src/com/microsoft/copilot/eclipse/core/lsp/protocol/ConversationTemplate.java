// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

/**
 * Represents a conversation template returned by the language server.
 */
public record ConversationTemplate(
    String id,
    String description,
    String shortDescription,
    List<String> scopes,
    TemplateSource source) {
}
