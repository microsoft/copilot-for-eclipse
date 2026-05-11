// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.util.HashMap;
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
  private static final String TOOL_NAME = "edit_file";
  private static final String SUBAGENT_TOOL_NAME = "run_subagent";
  private static final String ERROR_MESSAGE = "file not found";
  private static final String PROGRESS_MESSAGE = "Editing file.txt";

  private Shell shell;
  private MockedStatic<CopilotUi> copilotUiMock;
  private CopilotUi mockPlugin;

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

    // Mockito's static mocks are scoped to the registering thread. The widgets under
    // test execute on the SWT Display thread (via SwtUtils.invokeOnDisplayThread), so
    // the mock must be registered there as well; otherwise CopilotUi.getPlugin() calls
    // from the display thread would not see it.
    SwtUtils.invokeOnDisplayThread(() -> {
      shell = new Shell(Display.getDefault());
      copilotUiMock = mockStatic(CopilotUi.class);
      mockPlugin = mock(CopilotUi.class);
      copilotUiMock.when(CopilotUi::getPlugin).thenReturn(mockPlugin);
      lenient().when(mockPlugin.getChatServiceManager()).thenReturn(mockChatServiceManager);
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (copilotUiMock != null) {
        copilotUiMock.close();
        copilotUiMock = null;
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Test
  void appendToolCallStatus_errorWithMessage_rendersErrorText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", PROGRESS_MESSAGE, ERROR_MESSAGE));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text, "Status label text must be created when an error event is delivered");
      assertTrue(text.getText().contains(ERROR_MESSAGE),
          "Expected error message to be rendered next to the error icon, got: '" + text.getText() + "'");
      assertTrue(text.getText().contains(TOOL_NAME),
          "Expected error text to be prefixed with the tool name, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_errorWithoutProgressMessage_isStillProcessed() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // No progressMessage (e.g., tool failed before the running event was dispatched server-side).
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", null, ERROR_MESSAGE));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text, "Error event without progressMessage must still be processed");
      assertTrue(text.getText().contains(ERROR_MESSAGE),
          "Expected error message to be rendered, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_runningThenError_replacesProgressTextWithErrorText() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // First the running event sets the progress text.
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "running", PROGRESS_MESSAGE, null));
      StyledText runningText = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(runningText, "Running event should have populated text");
      assertTrue(runningText.getText().contains(PROGRESS_MESSAGE),
          "Running text should be visible before failure, got: '" + runningText.getText() + "'");

      // Then the same tool call fails: the error text must overwrite the stale running text.
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", PROGRESS_MESSAGE, ERROR_MESSAGE));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text);
      assertTrue(text.getText().contains(ERROR_MESSAGE),
          "Error text should replace stale running text, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_errorFallsBackToProgressMessage_whenErrorIsBlank() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // Whitespace-only error must fall back to progressMessage rather than rendering empty text.
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", PROGRESS_MESSAGE, "   "));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text, "Status label text must be created on error even when only progressMessage is present");
      assertTrue(text.getText().contains(PROGRESS_MESSAGE),
          "Expected progressMessage to be used as fallback, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_errorFallsBackToProgressMessage_whenErrorIsNull() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", PROGRESS_MESSAGE, null));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text);
      assertTrue(text.getText().contains(PROGRESS_MESSAGE),
          "Expected progressMessage to be used as fallback, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_errorWithBothFieldsBlank_rendersGenericFallbackWithToolName() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "error", null, null));

      StyledText text = getStatusLabelText(widget, TOOL_CALL_ID);
      assertNotNull(text, "Error event with no fields must still surface a generic message");
      assertTrue(text.getText().contains(TOOL_NAME),
          "Generic fallback should still tell the user which tool failed, got: '" + text.getText() + "'");
    });
  }

  @Test
  void appendToolCallStatus_nonErrorStatusWithOnlyError_isIgnored() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // Non-error events with only `error` populated must be dropped; otherwise
      // setRunningStatus/setCompletedStatus/setText would call setMarkup(null).
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "running", null, ERROR_MESSAGE));
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "completed", "   ", ERROR_MESSAGE));
      widget.appendToolCallStatus(buildToolCall(TOOL_NAME, "cancelled", null, ERROR_MESSAGE));

      @SuppressWarnings("unchecked")
      Map<String, AgentStatusLabel> labels = (Map<String, AgentStatusLabel>) getField(widget, "statusLabels");
      assertTrue(labels.isEmpty(),
          "Non-error events without progressMessage should be ignored, got: " + labels.keySet());
    });
  }

  /**
   * Regression: a {@code run_subagent} terminal event with no progressMessage must still
   * clear the subagent block state. Otherwise subsequent messages get routed to a ghost
   * {@code currentSubagentBlock} (see review comment on PR #145).
   */
  @Test
  void appendToolCallStatus_subagentTerminalEventWithBlankMessage_clearsSubagentState() {
    SwtUtils.invokeOnDisplayThread(() -> {
      CopilotTurnWidget widget = new CopilotTurnWidget(shell, SWT.NONE, mockChatServiceManager, TURN_ID);

      // Open a subagent block.
      widget.appendToolCallStatus(buildToolCall(SUBAGENT_TOOL_NAME, "running", null, null));
      assertTrue((boolean) getField(widget, "inSubagentBlock"),
          "Subagent block should be active after a 'running' event");
      assertNotNull(getField(widget, "currentSubagentBlock"),
          "currentSubagentBlock should be populated after a 'running' event");

      // Terminal event with no display message — the early-return guard for blank
      // progressMessage must NOT short-circuit subagent state cleanup.
      widget.appendToolCallStatus(buildToolCall(SUBAGENT_TOOL_NAME, "completed", null, null));

      assertFalse((boolean) getField(widget, "inSubagentBlock"),
          "inSubagentBlock should be cleared after a terminal subagent event");
      assertNull(getField(widget, "currentSubagentBlock"),
          "currentSubagentBlock should be cleared after a terminal subagent event");
    });
  }

  private static AgentToolCall buildToolCall(String name, String status, String progressMessage, String error) {
    Gson gson = new Gson();
    Map<String, Object> fields = new HashMap<>();
    fields.put("id", TOOL_CALL_ID);
    fields.put("name", name);
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

