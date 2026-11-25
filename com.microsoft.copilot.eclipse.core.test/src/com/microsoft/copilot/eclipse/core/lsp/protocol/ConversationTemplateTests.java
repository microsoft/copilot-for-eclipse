// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class ConversationTemplateTests {

  private final Gson gson = new Gson();

  @Test
  void testCommandName_promptFile_returnsName() {
    String json = """
        {
          "id": "file:///c%3A/repo/.github/prompts/review.prompt.md",
          "name": "review",
          "description": "",
          "scopes": ["chat-panel", "agent-panel"],
          "source": "prompt"
        }
        """;

    ConversationTemplate template = gson.fromJson(json, ConversationTemplate.class);

    assertEquals("review", template.commandName());
  }

  @Test
  void testCommandName_promptFileWithoutName_returnsId() {
    ConversationTemplate template = new ConversationTemplate("file:///repo/review.prompt.md", null, null, null,
        List.of(), TemplateSource.PROMPT);

    assertEquals("file:///repo/review.prompt.md", template.commandName());
  }

  @Test
  void testCommandName_skill_returnsId() {
    ConversationTemplate template = new ConversationTemplate("skill:marker-skill", "marker-skill", null, null,
        List.of(), TemplateSource.SKILL);

    assertEquals("skill:marker-skill", template.commandName());
  }

  @Test
  void testCommandName_builtin_returnsId() {
    ConversationTemplate template = new ConversationTemplate("explain", null, null, null, List.of(),
        TemplateSource.BUILTIN);

    assertEquals("explain", template.commandName());
  }
}
