// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a conversation template returned by the language server.
 */
public record ConversationTemplate(
    String id,
    String name,
    String description,
    String shortDescription,
    List<String> scopes,
    TemplateSource source) {

  /**
   * Returns the name to invoke this template as slash command. Prompt files are identified by their file URI, but the
   * language server resolves them by their name.
   *
   * @return the slash command name without the leading slash
   */
  public String commandName() {
    return source == TemplateSource.PROMPT && StringUtils.isNotBlank(name) ? name : id;
  }
}
