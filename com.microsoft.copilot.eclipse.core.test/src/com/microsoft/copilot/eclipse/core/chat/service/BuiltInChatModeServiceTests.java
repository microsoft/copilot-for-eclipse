package com.microsoft.copilot.eclipse.core.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;

@ExtendWith(MockitoExtension.class)
class BuiltInChatModeServiceTests {

  @Mock
  private CopilotLanguageServerConnection mockConnection;

  private BuiltInChatModeService builtInChatModeService;

  @BeforeEach
  void setUp() throws Exception {
    builtInChatModeService = new BuiltInChatModeService();

    CopilotCore plugin = new CopilotCore();
    Field languageServerField = CopilotCore.class.getDeclaredField("copilotLanguageServer");
    languageServerField.setAccessible(true);
    languageServerField.set(plugin, mockConnection);
  }

  @Test
  void testLoadBuiltInModes_inlineAgentSkippedFromAgentModes() {
    ConversationMode agentMode = createBuiltInMode("Agent", "Agent", "Agent",
        "Advanced agent mode with access to tools and capabilities");
    ConversationMode inlineAgentMode = createBuiltInMode("InlineAgent", "Agent", "InlineAgent",
        "Agent mode with a restricted tool set for inline editing");

    when(mockConnection.listConversationModes(any(ConversationModesParams.class)))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] { agentMode, inlineAgentMode }));

    List<BuiltInChatMode> builtInModes = builtInChatModeService.loadBuiltInModes().join();

    assertEquals(1, builtInModes.size());

    BuiltInChatMode builtInMode = builtInModes.get(0);
    assertNotNull(builtInMode);
    assertEquals("Agent", builtInMode.getId());
    assertEquals("Agent", builtInMode.getDisplayName());
    assertEquals("Agent", builtInMode.getKind());
  }

  private ConversationMode createBuiltInMode(String id, String name, String kind, String description) {
    ConversationMode mode = new ConversationMode();
    mode.setId(id);
    mode.setName(name);
    mode.setKind(kind);
    mode.setBuiltIn(true);
    mode.setDescription(description);
    return mode;
  }
}
