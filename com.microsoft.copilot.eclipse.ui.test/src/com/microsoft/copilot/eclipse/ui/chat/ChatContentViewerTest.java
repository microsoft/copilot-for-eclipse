// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentRound;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ChatContentViewerTest {

  private static final String TURN_ID = "239ca6fd-f4c9-440d-869e-eb93eef7c522";
  private static final String CONVERSATION_ID = "8d8f3177-6ebd-4cec-9ae2-81d2c52979b4";
  private static final String EXPECTED_REPLY =
      "Hello! How can I assist you with your project today?";

  private Shell shell;
  private ChatContentViewer viewer;

  @Mock
  private ChatServiceManager mockChatServiceManager;

  @BeforeEach
  void setUp() {
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      viewer = new ChatContentViewer(shell, SWT.NONE, mockChatServiceManager);
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void testProcessTurnEvent_agentRoundReplyIsAppendedToTurnWidget() {
    SwtUtils.invokeOnDisplayThread(() -> {
      // Create a mock turn widget and inject into the turns map
      BaseTurnWidget mockTurnWidget = mock(BaseTurnWidget.class);
      getTurnsMap(viewer).put(TURN_ID, mockTurnWidget);

      // Build a ChatProgressValue matching the LSP response
      ChatProgressValue value = buildAgentRoundProgressValue();

      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        viewer.processTurnEvent(value);
      }

      verify(mockTurnWidget).appendMessage(EXPECTED_REPLY);
    });
  }

  private ChatProgressValue buildAgentRoundProgressValue() {
    ChatProgressValue value = new ChatProgressValue();
    value.setKind(WorkDoneProgressKind.report);
    value.setTurnId(TURN_ID);
    value.setConversationId(CONVERSATION_ID);

    // AgentRound has no setters, use Gson to construct it
    String agentRoundJson = new Gson().toJson(Map.of("roundId", 1, "reply", EXPECTED_REPLY));
    AgentRound agentRound = new Gson().fromJson(agentRoundJson, AgentRound.class);

    // ChatProgressValue uses 'editAgentRounds' field (no setter), set via reflection
    setFieldValue(value, "editAgentRounds", List.of(agentRound));
    return value;
  }

  @SuppressWarnings("unchecked")
  private Map<String, BaseTurnWidget> getTurnsMap(ChatContentViewer viewer) {
    return (Map<String, BaseTurnWidget>) getFieldValue(viewer, "turns");
  }

  private Object getFieldValue(Object target, String fieldName) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get field " + fieldName, e);
    }
  }

  private void setFieldValue(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }
}
