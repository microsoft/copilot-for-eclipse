// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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

  @Test
  void testClampOffset_clampsToScrollableRange() {
    SwtUtils.invokeOnDisplayThread(() -> {
      sizeViewer(400, 300);
      setFieldValue(viewer, "totalHeight", 1000);
      int maxOffset = invokeIntMethod(viewer, "maxOffset");

      Assertions.assertTrue(maxOffset > 0, "content taller than the viewport should be scrollable");
      Assertions.assertEquals(0, invokeIntMethod(viewer, "clampOffset", -50),
          "negative offset should clamp to 0");
      Assertions.assertEquals(maxOffset, invokeIntMethod(viewer, "clampOffset", 5000),
          "offset past the end should clamp to maxOffset");
      Assertions.assertEquals(200, invokeIntMethod(viewer, "clampOffset", 200),
          "in-range offset should be left unchanged");
    });
  }

  @Test
  void testMaxOffset_isZeroWhenContentFitsViewport() {
    SwtUtils.invokeOnDisplayThread(() -> {
      sizeViewer(400, 300);
      setFieldValue(viewer, "totalHeight", 200);

      Assertions.assertEquals(0, invokeIntMethod(viewer, "maxOffset"),
          "content shorter than the viewport should not be scrollable");
    });
  }

  @Test
  void testUpdateScrollBar_rangeMatchesTotalHeight() {
    SwtUtils.invokeOnDisplayThread(() -> {
      sizeViewer(400, 300);
      setFieldValue(viewer, "totalHeight", 1000);
      setFieldValue(viewer, "scrollOffset", 120);

      invokeVoidMethod(viewer, "updateScrollBar", 300);

      ScrollBar bar = viewer.getVerticalBar();
      Assertions.assertTrue(bar.getEnabled(), "scrollbar should be enabled when content overflows");
      Assertions.assertEquals(1000, bar.getMaximum(), "scrollbar maximum should equal totalHeight");
      Assertions.assertEquals(300, bar.getThumb(), "scrollbar thumb should equal the viewport height");
      Assertions.assertEquals(120, bar.getSelection(), "scrollbar selection should equal scrollOffset");
    });
  }

  @Test
  void testUpdateScrollBar_disabledWhenContentFits() {
    SwtUtils.invokeOnDisplayThread(() -> {
      sizeViewer(400, 300);
      setFieldValue(viewer, "totalHeight", 200);

      invokeVoidMethod(viewer, "updateScrollBar", 300);

      Assertions.assertFalse(viewer.getVerticalBar().getEnabled(),
          "scrollbar should be disabled when all content fits");
    });
  }

  @Test
  void testIsViewportAtBottom_togglesWhenLeavingAndReturning() {
    SwtUtils.invokeOnDisplayThread(() -> {
      sizeViewer(400, 300);
      setFieldValue(viewer, "totalHeight", 1000);
      int maxOffset = invokeIntMethod(viewer, "maxOffset");

      setFieldValue(viewer, "scrollOffset", maxOffset);
      Assertions.assertTrue(invokeBooleanMethod(viewer, "isViewportAtBottom"),
          "pinned to the very bottom should report at-bottom");

      setFieldValue(viewer, "scrollOffset", 0);
      Assertions.assertFalse(invokeBooleanMethod(viewer, "isViewportAtBottom"),
          "scrolled up to the top should report not-at-bottom");

      // Just inside the bottom threshold (SCROLL_THRESHOLD = 100) should count as at-bottom again.
      setFieldValue(viewer, "scrollOffset", maxOffset - 50);
      Assertions.assertTrue(invokeBooleanMethod(viewer, "isViewportAtBottom"),
          "back within the bottom threshold should report at-bottom again");
    });
  }

  /** Gives the viewer (and its content child) a deterministic client area for scroll-model math. */
  private void sizeViewer(int width, int height) {
    viewer.setSize(width, height);
    viewer.layout(true, true);
  }

  private int invokeIntMethod(Object target, String name, int arg) {
    return (int) invokeMethod(target, name, new Class<?>[] {int.class}, arg);
  }

  private int invokeIntMethod(Object target, String name) {
    return (int) invokeMethod(target, name, new Class<?>[] {});
  }

  private boolean invokeBooleanMethod(Object target, String name) {
    return (boolean) invokeMethod(target, name, new Class<?>[] {});
  }

  private void invokeVoidMethod(Object target, String name, int arg) {
    invokeMethod(target, name, new Class<?>[] {int.class}, arg);
  }

  private Object invokeMethod(Object target, String name, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = target.getClass().getDeclaredMethod(name, paramTypes);
      method.setAccessible(true);
      return method.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke method " + name, e);
    }
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
