// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Verifies that {@link BaseTurnWidget#appendToolCallStatus} renders text for
 * tool calls that finish with status {@code error}.
 */
@ExtendWith(MockitoExtension.class)
class BaseTurnWidgetToolCallStatusTest {

  private static final String TURN_ID = "turn-1";
  private static final String TOOL_CALL_ID = "tool-call-1";
  private static final String ERROR_MESSAGE = "file not found";
  private static final String PROGRESS_MESSAGE = "Editing file.txt";

  private Shell shell;

  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private AvatarService mockAvatarService;
  @Mock
  private ChatFontService mockChatFontService;

  @BeforeEach
  void setUp() {
    lenient().when(mockChatServiceManager.getAvatarService()).thenReturn(mockAvatarService);
    lenient().when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);
    lenient().when(mockAvatarService.getAvatarForCopilot()).thenReturn(null);

    SwtUtils.invokeOnDisplayThread(() -> shell = new Shell(Display.getDefault()));
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
  void appendToolCallStatus_errorWithMessage_rendersErrorText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

        widget.appendToolCallStatus(buildToolCall("error", PROGRESS_MESSAGE, ERROR_MESSAGE));

        StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(text, "Status label text must be created when an error event is delivered");
        assertTrue(text.getText().contains(ERROR_MESSAGE),
            "Expected error message to be rendered next to the error icon, got: '" + text.getText() + "'");
      }
    });
  }

  @Test
  void appendToolCallStatus_errorWithoutProgressMessage_isStillProcessed() {
    SwtUtils.invokeOnDisplayThread(() -> {
      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

        // No progressMessage (e.g., tool failed before the running event was dispatched server-side).
        widget.appendToolCallStatus(buildToolCall("error", null, ERROR_MESSAGE));

        StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(text, "Error event without progressMessage must still be processed");
        assertTrue(text.getText().contains(ERROR_MESSAGE),
            "Expected error message to be rendered, got: '" + text.getText() + "'");
      }
    });
  }

  @Test
  void appendToolCallStatus_runningThenError_replacesProgressTextWithErrorText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

        // First the running event sets the progress text.
        widget.appendToolCallStatus(buildToolCall("running", PROGRESS_MESSAGE, null));
        StyledText runningText = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(runningText, "Running event should have populated text");
        assertTrue(runningText.getText().contains(PROGRESS_MESSAGE),
            "Running text should be visible before failure, got: '" + runningText.getText() + "'");

        // Then the same tool call fails: the error text must overwrite the stale running text.
        widget.appendToolCallStatus(buildToolCall("error", PROGRESS_MESSAGE, ERROR_MESSAGE));

        StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(text);
        assertTrue(text.getText().contains(ERROR_MESSAGE),
            "Error text should replace stale running text, got: '" + text.getText() + "'");
      }
    });
  }

  @Test
  void appendToolCallStatus_errorFallsBackToProgressMessage_whenErrorIsBlank() {
    SwtUtils.invokeOnDisplayThread(() -> {
      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

        // Whitespace-only error must fall back to progressMessage rather than rendering empty text.
        widget.appendToolCallStatus(buildToolCall("error", PROGRESS_MESSAGE, "   "));

        StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(text, "Status label text must be created on error even when only progressMessage is present");
        assertTrue(text.getText().contains(PROGRESS_MESSAGE),
            "Expected progressMessage to be used as fallback, got: '" + text.getText() + "'");
      }
    });
  }

  @Test
  void appendToolCallStatus_errorFallsBackToProgressMessage_whenErrorIsNull() {
    SwtUtils.invokeOnDisplayThread(() -> {
      try (MockedStatic<CopilotUi> copilotUiMock = mockStatic(CopilotUi.class)) {
        CopilotUi mockPlugin = mock(CopilotUi.class);
        copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
        lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);

        CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

        widget.appendToolCallStatus(buildToolCall("error", PROGRESS_MESSAGE, null));

        StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
        assertNotNull(text);
        assertTrue(text.getText().contains(PROGRESS_MESSAGE),
            "Expected progressMessage to be used as fallback, got: '" + text.getText() + "'");
      }
    });
  }

  private static AgentToolCall buildToolCall(String status, String progressMessage, String error) {
    Gson gson = new Gson();
    java.util.Map<String, Object> fields = new java.util.HashMap<>();
    fields.put("id", TOOL_CALL_ID);
    fields.put("name", "edit_file");
    fields.put("status", status);
    if (progressMessage != null) {
      fields.put("progressMessage", progressMessage);
    }
    if (error != null) {
      fields.put("error", error);
    }
    return gson.fromJson(gson.toJson(fields), AgentToolCall.class);
  }

  private static StyledText getStatusLabelText(BaseTurnWidget widget, String toolCallId) {
    @SuppressWarnings("unchecked")
    Map<String, AgentStatusLabel> labels = (Map<String, AgentStatusLabel>) getField(widget, "statusLabels");
    AgentStatusLabel label = labels.get(toolCallId);
    assertNotNull(label, "AgentStatusLabel for tool call '" + toolCallId + "' should have been created");
    Object markupViewer = getField(label, "textLabel");
    if (markupViewer == null) {
      return null;
    }
    try {
      return (StyledText) markupViewer.getClass().getMethod("getTextWidget").invoke(markupViewer);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to obtain StyledText from textLabel", e);
    }
  }

  private static Object getField(Object target, String name) {
    Class<?> cls = target.getClass();
    while (cls != null) {
      try {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
      } catch (NoSuchFieldException e) {
        cls = cls.getSuperclass();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Field '" + name + "' not found on " + target.getClass());
  }
}
