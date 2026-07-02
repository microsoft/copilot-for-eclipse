// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.WorkDoneProgressKind;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentRound;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationError;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockState;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

/**
 * Behavior tests for {@link BrowserConversationWidget} covering turn lifecycle,
 * state tracking, tool confirmation logic, and error handling.
 *
 * <p>These tests run as Eclipse plugin tests (the widget constructor requires the
 * CopilotUi bundle to be active for icon loading). The browser page may not fully
 * load in the headless test environment, but state-tracking logic and the script
 * queuing path are still exercisable.
 */
class BrowserConversationWidgetBehaviorTest {

  private Shell shell;
  private BrowserConversationWidget widget;

  @BeforeEach
  void setUp() {
    Display display = Display.getDefault();
    display.syncExec(() -> {
      shell = new Shell(display);
      widget = new BrowserConversationWidget(shell, mockServiceManager());
    });
  }

  /**
   * Builds a mock {@link ChatServiceManager} whose {@link AvatarService} returns non-null avatar
   * data URIs and display names, mirroring the non-null service manager the widget receives in
   * production.
   */
  private static ChatServiceManager mockServiceManager() {
    AvatarService avatarService = Mockito.mock(AvatarService.class);
    Mockito.when(avatarService.getAvatarForCopilotAsDataUri()).thenReturn("");
    Mockito.when(avatarService.getAvatarForCurrentUserAsDataUri()).thenReturn("");
    Mockito.when(avatarService.getUserName()).thenReturn("User");
    Mockito.when(avatarService.getCopilotName()).thenReturn("GitHub Copilot");
    ChatServiceManager manager = Mockito.mock(ChatServiceManager.class);
    Mockito.when(manager.getAvatarService()).thenReturn(avatarService);
    return manager;
  }

  @AfterEach
  void tearDown() {
    Display.getDefault().syncExec(() -> {
      if (widget != null && !widget.isDisposed()) {
        widget.dispose();
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
  }

  @Nested
  class TurnLifecycleTests {

    @Test
    void processTurnEvent_thinking_setsActiveThinkingBlockId() {
      widget.beginTurn("turn-1", true, false);

      ChatProgressValue event = new ChatProgressValue();
      event.setKind(WorkDoneProgressKind.report);
      event.setTurnId("turn-1");
      event.setThinking(new Thinking("think-1", "Analyzing...", null));
      widget.processTurnEvent(event);

      assertNotNull(widget.getActiveThinkingBlockId("turn-1"));
    }

    @Test
    void processTurnEvent_multipleThinkingChunks_maintainsSameBlockId() {
      widget.beginTurn("turn-1", true, false);

      ChatProgressValue event1 = new ChatProgressValue();
      event1.setKind(WorkDoneProgressKind.report);
      event1.setTurnId("turn-1");
      event1.setThinking(new Thinking("think-1", "First chunk. ", null));
      widget.processTurnEvent(event1);
      String blockId1 = widget.getActiveThinkingBlockId("turn-1");

      ChatProgressValue event2 = new ChatProgressValue();
      event2.setKind(WorkDoneProgressKind.report);
      event2.setTurnId("turn-1");
      event2.setThinking(new Thinking("think-1", "Second chunk.", null));
      widget.processTurnEvent(event2);
      String blockId2 = widget.getActiveThinkingBlockId("turn-1");

      assertEquals(blockId1, blockId2, "Subsequent thinking chunks should use same block");
    }

    @Test
    void processTurnEvent_thinkingSealedByReplyInSameEvent() {
      widget.beginTurn("turn-1", true, false);

      // First: establish a thinking block
      ChatProgressValue thinkEvent = new ChatProgressValue();
      thinkEvent.setKind(WorkDoneProgressKind.report);
      thinkEvent.setTurnId("turn-1");
      thinkEvent.setThinking(new Thinking("think-1", "Planning...", null));
      widget.processTurnEvent(thinkEvent);
      assertNotNull(widget.getActiveThinkingBlockId("turn-1"));

      // Now: event with both thinking continuation AND a reply (triggers seal)
      ChatProgressValue sealEvent = new ChatProgressValue();
      sealEvent.setKind(WorkDoneProgressKind.report);
      sealEvent.setTurnId("turn-1");
      sealEvent.setThinking(new Thinking("think-1", "Done planning.", null));
      sealEvent.setReply("Here is the answer.");
      widget.processTurnEvent(sealEvent);

      // Thinking block should be sealed (cleared from active tracking)
      assertNull(widget.getActiveThinkingBlockId("turn-1"));
    }

    @Test
    void processTurnEvent_end_clearsThinkingState() {
      widget.beginTurn("turn-1", true, false);

      ChatProgressValue thinkEvent = new ChatProgressValue();
      thinkEvent.setKind(WorkDoneProgressKind.report);
      thinkEvent.setTurnId("turn-1");
      thinkEvent.setThinking(new Thinking("think-1", "Thinking...", null));
      widget.processTurnEvent(thinkEvent);

      ChatProgressValue endEvent = new ChatProgressValue();
      endEvent.setKind(WorkDoneProgressKind.end);
      endEvent.setTurnId("turn-1");
      widget.processTurnEvent(endEvent);

      assertNull(widget.getActiveThinkingBlockId("turn-1"));
    }

    @Test
    void processTurnEvent_differentTurnId_tracksThinkingPerTurn() {
      widget.beginTurn("turn-1", true, false);

      ChatProgressValue thinkEvent = new ChatProgressValue();
      thinkEvent.setKind(WorkDoneProgressKind.report);
      thinkEvent.setTurnId("turn-1");
      thinkEvent.setThinking(new Thinking("think-1", "Thinking...", null));
      widget.processTurnEvent(thinkEvent);
      String turn1Thinking = widget.getActiveThinkingBlockId("turn-1");
      assertNotNull(turn1Thinking);

      // A report for a different turn must not disturb turn-1's per-turn state.
      ChatProgressValue otherEvent = new ChatProgressValue();
      otherEvent.setKind(WorkDoneProgressKind.report);
      otherEvent.setTurnId("turn-2");
      otherEvent.setReply("Hello");
      widget.processTurnEvent(otherEvent);

      assertEquals(turn1Thinking, widget.getActiveThinkingBlockId("turn-1"),
          "Each turn tracks its own thinking block independently");
      assertNull(widget.getActiveThinkingBlockId("turn-2"),
          "turn-2 produced only a reply, so it has no thinking block");
    }

    @Test
    void processTurnEvent_toolCallFollowedByReply_transitionsBlockType() {
      widget.beginTurn("turn-1", true, false);

      // Tool call event (AgentToolCall/AgentRound have no setters — use reflection)
      AgentToolCall toolCall = new AgentToolCall();
      setField(toolCall, "id", "tc-1");
      setField(toolCall, "name", "file_search");
      setField(toolCall, "status", "running");
      AgentRound round = new AgentRound();
      setField(round, "toolCalls", List.of(toolCall));

      ChatProgressValue toolEvent = new ChatProgressValue();
      toolEvent.setKind(WorkDoneProgressKind.report);
      toolEvent.setTurnId("turn-1");
      setField(toolEvent, "editAgentRounds", List.of(round));
      widget.processTurnEvent(toolEvent);

      // Reply event following tool call
      ChatProgressValue replyEvent = new ChatProgressValue();
      replyEvent.setKind(WorkDoneProgressKind.report);
      replyEvent.setTurnId("turn-1");
      replyEvent.setReply("Here are the results.");
      widget.processTurnEvent(replyEvent);

      // No exception = state transition worked correctly
      assertNull(widget.getActiveThinkingBlockId("turn-1"));
    }

    @Test
    void processTurnEvent_errorMessageDuringStreaming() {
      widget.beginTurn("turn-1", true, false);

      // An error arrives on a report event (the streaming error path)
      ChatProgressValue errorEvent = new ChatProgressValue();
      errorEvent.setKind(WorkDoneProgressKind.report);
      errorEvent.setTurnId("turn-1");
      ConversationError error = new ConversationError();
      error.setMessage("Context window exceeded");
      error.setCode(400);
      errorEvent.setConversationError(error);
      widget.processTurnEvent(errorEvent);

      // No exception; error is rendered as warning block
    }

    @Test
    void processTurnEvent_errorOnEndEvent() {
      widget.beginTurn("turn-1", true, false);

      ChatProgressValue endEvent = new ChatProgressValue();
      endEvent.setKind(WorkDoneProgressKind.end);
      endEvent.setTurnId("turn-1");
      ConversationError error = new ConversationError();
      error.setMessage("Server disconnected");
      error.setCode(500);
      endEvent.setConversationError(error);
      widget.processTurnEvent(endEvent);

      // Error on end should still be processed without exception
    }
  }

  @Nested
  class ToolConfirmationTests {

    @Test
    void requestToolConfirmation_returnsPendingFuture() {
      widget.beginTurn("turn-1", true, false);

      ConfirmationContent content = new ConfirmationContent(
          "Run command?", "Execute npm install",
          List.of(ConfirmationAction.allowOnce("Allow"),
              ConfirmationAction.skip("Deny")));

      CompletableFuture<LanguageModelToolConfirmationResult> future =
          widget.requestToolConfirmation("turn-1", content, null);

      assertNotNull(future);
      assertFalse(future.isDone());
    }

    @Test
    void cancelToolConfirmation_completesFutureWithDismiss() throws Exception {
      widget.beginTurn("turn-1", true, false);

      ConfirmationContent content = new ConfirmationContent(
          "Run command?", "Execute npm install",
          List.of(ConfirmationAction.allowOnce("Allow")));

      CompletableFuture<LanguageModelToolConfirmationResult> future =
          widget.requestToolConfirmation("turn-1", content, null);

      widget.cancelToolConfirmation("turn-1");

      assertTrue(future.isDone());
      // getResult() is the LSP wire value ("dismiss"), not the enum constant.
      assertEquals(ToolConfirmationResult.DISMISS.getValue(),
          future.get(1, TimeUnit.SECONDS).getResult());
    }

    @Test
    void requestToolConfirmation_cancelsPreviousPendingConfirmation() throws Exception {
      widget.beginTurn("turn-1", true, false);

      ConfirmationContent content1 = new ConfirmationContent(
          "First?", "First action",
          List.of(ConfirmationAction.allowOnce("Allow")));
      CompletableFuture<LanguageModelToolConfirmationResult> future1 =
          widget.requestToolConfirmation("turn-1", content1, null);

      ConfirmationContent content2 = new ConfirmationContent(
          "Second?", "Second action",
          List.of(ConfirmationAction.allowOnce("Allow")));
      CompletableFuture<LanguageModelToolConfirmationResult> future2 =
          widget.requestToolConfirmation("turn-1", content2, null);

      assertTrue(future1.isDone(), "First future should be auto-cancelled");
      assertFalse(future2.isDone(), "Second future should still be pending");
      // getResult() is the LSP wire value ("dismiss"), not the enum constant.
      assertEquals(ToolConfirmationResult.DISMISS.getValue(),
          future1.get(1, TimeUnit.SECONDS).getResult());
    }

    @Test
    void cancelToolConfirmation_whenNoPending_doesNotThrow() {
      assertDoesNotThrow(() -> widget.cancelToolConfirmation("turn-1"));
    }

    @Test
    void getLastSelectedConfirmationAction_returnsNullBeforeAnyConfirmation() {
      assertNull(widget.getLastSelectedConfirmationAction());
    }
  }

  @Nested
  class RestoreTurnTests {

    private ConversationDataFactory dataFactory;

    @BeforeEach
    void setUpFactory() {
      AuthStatusManager mockAuth = Mockito.mock(AuthStatusManager.class);
      Mockito.when(mockAuth.getUserName()).thenReturn("test-user");
      dataFactory = new ConversationDataFactory(mockAuth);
    }

    @Test
    void restoreTurn_null_doesNotThrow() {
      assertDoesNotThrow(() -> widget.restoreTurn(null, dataFactory));
    }

    @Test
    void restoreTurn_userTurnWithNullMessage_doesNotRender() {
      UserTurnData userTurn = new UserTurnData();
      userTurn.setTurnId("turn-null-msg");
      userTurn.setMessage(null);
      assertDoesNotThrow(() -> widget.restoreTurn(userTurn, dataFactory));
    }

    @Test
    void restoreTurn_copilotTurnWithParentTurnId_skipsRendering() {
      CopilotTurnData copilotTurn = new CopilotTurnData();
      copilotTurn.setTurnId("turn-child");
      copilotTurn.setParentTurnId("turn-parent");
      assertDoesNotThrow(() -> widget.restoreTurn(copilotTurn, dataFactory));
    }

    @Test
    void restoreTurn_fullCopilotTurnWithAllBlockTypes() {
      CopilotTurnData copilotTurn = new CopilotTurnData();
      copilotTurn.setTurnId("turn-full");
      ReplyData reply = copilotTurn.getReply();
      reply.setText("Here is a **formatted** reply with `code`.");
      reply.setModelName("GPT-5 mini");
      reply.setBillingMultiplier(1.0);
      reply.setReasoningEffort("medium");

      // Thinking block
      ThinkingBlockData thinking = new ThinkingBlockData();
      thinking.setId("think-full");
      thinking.setContent("Analyzing requirements...");
      thinking.setTitle("Planning");
      thinking.setState(ThinkingBlockState.COMPLETED);

      // Tool calls
      ToolCallData completedTool = new ToolCallData();
      completedTool.setId("tc-1");
      completedTool.setName("file_search");
      completedTool.setStatus("completed");
      completedTool.setProgressMessage("Found 5 results");

      ToolCallData errorTool = new ToolCallData();
      errorTool.setId("tc-2");
      errorTool.setName("edit_file");
      errorTool.setStatus("error");
      errorTool.setProgressMessage("Permission denied");

      EditAgentRoundData round = new EditAgentRoundData();
      round.setRoundId(1);
      round.setThinkingBlock(thinking);
      round.setToolCalls(List.of(completedTool, errorTool));
      round.setReply("After tool execution, here are more details.");
      reply.setEditAgentRounds(List.of(round));

      // Error messages
      ErrorData errorData = new ErrorData();
      errorData.setMessage("Rate limited");
      errorData.setCode(429);
      ErrorMessageData errorMsg = new ErrorMessageData();
      errorMsg.setError(errorData);
      reply.setErrorMessages(List.of(errorMsg));

      assertDoesNotThrow(() -> widget.restoreTurn(copilotTurn, dataFactory));
    }

    @Test
    void restoreTurn_copilotTurnWithNullReplyData() {
      CopilotTurnData copilotTurn = new CopilotTurnData();
      copilotTurn.setTurnId("turn-no-reply");
      // getReply() returns a new ReplyData with all nulls by default —
      // but the text/model fields are blank so no content blocks are created
      assertDoesNotThrow(() -> widget.restoreTurn(copilotTurn, dataFactory));
    }
  }

  @Nested
  class CompactingStatusTests {

    @Test
    void showAndHideCompactingStatus_tracksLastCopilotTurnId() {
      // Begin a copilot turn so lastCopilotTurnId is set
      widget.beginTurn("turn-1", true, false);

      // These methods depend on lastCopilotTurnId being set correctly
      assertDoesNotThrow(() -> widget.showCompactingStatusOnLatestCopilotTurn());
      assertDoesNotThrow(() -> widget.hideCompactingStatusOnLatestCopilotTurn());
    }

    @Test
    void showCompactingStatus_beforeAnyTurn_doesNotThrow() {
      // No turn started yet — should handle gracefully
      assertDoesNotThrow(() -> widget.showCompactingStatusOnLatestCopilotTurn());
    }

    @Test
    void showCompactingStatus_afterUserTurn_usesLastCopilotTurn() {
      widget.beginTurn("turn-1", true, false);
      widget.beginTurn("turn-2", false, false); // user turn

      // Should still reference turn-1 (last copilot turn)
      assertDoesNotThrow(() -> widget.showCompactingStatusOnLatestCopilotTurn());
      assertDoesNotThrow(() -> widget.hideCompactingStatusOnLatestCopilotTurn());
    }
  }

  @Nested
  class WidgetDisposalDuringOperationsTests {

    @Test
    void dispose_duringPendingConfirmation_doesNotThrow() {
      widget.beginTurn("turn-1", true, false);

      ConfirmationContent content = new ConfirmationContent(
          "Run?", "Command",
          List.of(ConfirmationAction.allowOnce("Allow")));
      CompletableFuture<LanguageModelToolConfirmationResult> future =
          widget.requestToolConfirmation("turn-1", content, null);

      // Dispose while confirmation is pending (SWT disposal must run on the UI thread).
      assertDoesNotThrow(() -> runOnUiThread(widget::dispose));
      // Future is left incomplete (no automatic completion on dispose)
      // This is acceptable — the caller (ChatView) manages lifecycle
    }

    @Test
    void processTurnEvent_afterDispose_doesNotThrow() {
      widget.beginTurn("turn-1", true, false);
      runOnUiThread(widget::dispose);

      ChatProgressValue event = new ChatProgressValue();
      event.setKind(WorkDoneProgressKind.report);
      event.setTurnId("turn-1");
      event.setReply("Should be ignored");

      // executeScript checks isDisposed() — should not throw
      assertDoesNotThrow(() -> widget.processTurnEvent(event));
    }

    @Test
    void restoreTurn_afterDispose_doesNotThrow() {
      runOnUiThread(widget::dispose);

      AuthStatusManager mockAuth = Mockito.mock(AuthStatusManager.class);
      Mockito.when(mockAuth.getUserName()).thenReturn("test-user");
      ConversationDataFactory dataFactory = new ConversationDataFactory(mockAuth);

      UserTurnData userTurn = new UserTurnData();
      userTurn.setTurnId("turn-1");
      userTurn.setMessage(new MessageData("Hello"));

      assertDoesNotThrow(() -> widget.restoreTurn(userTurn, dataFactory));
    }
  }

  @Nested
  class RenderOperationTests {

    @Test
    void renderErrorMessage_doesNotThrow() {
      assertDoesNotThrow(() -> widget.renderErrorMessage("Something went wrong"));
    }

    @Test
    void renderErrorMessage_withMarkdownContent() {
      assertDoesNotThrow(
          () -> widget.renderErrorMessage("**Error**: `connection refused`"));
    }

    @Test
    void startNewUserTurn_doesNotThrow() {
      assertDoesNotThrow(
          () -> widget.startNewUserTurn("turn-1", "How do I write tests?"));
    }

    @Test
    void renderAgentMessage_withNullParams_doesNotThrow() {
      assertDoesNotThrow(() -> widget.renderAgentMessage(null));
    }
  }

  /**
   * Subagent nesting: a subagent turn is rendered inside its parent copilot turn (driven by the
   * parent's {@code run_subagent} tool call), mirroring the SWT {@code BaseTurnWidget}. Because
   * streaming state is per-turn, the parent and subagent keep independent block counters, so their
   * block ids never collide.
   */
  @Nested
  class SubagentNestingTests {

    @Test
    void liveSubagentFlow_keepsParentAndSubagentStateIndependent() {
      // Parent copilot turn starts and launches a subagent.
      widget.beginTurn("parent", true, false);
      widget.processTurnEvent(
          toolCallReport("parent", null, "sub-tc", "run_subagent", "running"));

      // Subagent turn streams its own thinking, nested under the parent.
      widget.beginTurn("sub", true, false);
      widget.processTurnEvent(thinkingReport("sub", "parent", "Subagent planning..."));
      String subThinking = widget.getActiveThinkingBlockId("sub");
      assertNotNull(subThinking, "Subagent turn tracks its own thinking block");
      assertTrue(subThinking.startsWith("sub-"),
          "Subagent block ids derive from the subagent turn id (independent counter)");
      widget.processTurnEvent(endEvent("sub", "parent"));

      // Subagent completes; the parent resumes and continues thinking.
      widget.processTurnEvent(
          toolCallReport("parent", null, "sub-tc", "run_subagent", "completed"));
      widget.processTurnEvent(thinkingReport("parent", null, "Back in the parent..."));
      String parentThinking = widget.getActiveThinkingBlockId("parent");
      assertNotNull(parentThinking, "Parent turn keeps its own state after the subagent");
      assertTrue(parentThinking.startsWith("parent-"),
          "Parent block ids stay under the parent turn id");
      assertNotEquals(subThinking, parentThinking,
          "Parent and subagent block ids never collide");
    }

    @Test
    void restore_subagentTurnNestedUnderParent_doesNotThrow() {
      AuthStatusManager mockAuth = Mockito.mock(AuthStatusManager.class);
      Mockito.when(mockAuth.getUserName()).thenReturn("test-user");
      ConversationDataFactory dataFactory = new ConversationDataFactory(mockAuth);

      // Parent turn persisted with a run_subagent tool call.
      ToolCallData subToolCall = new ToolCallData();
      subToolCall.setId("sub-tc");
      subToolCall.setName("run_subagent");
      subToolCall.setStatus("completed");
      EditAgentRoundData parentRound = new EditAgentRoundData();
      parentRound.setToolCalls(List.of(subToolCall));
      parentRound.setReply("Delegating to a subagent.");
      ReplyData parentReply = new ReplyData();
      parentReply.setEditAgentRounds(List.of(parentRound));
      CopilotTurnData parentTurn = new CopilotTurnData();
      parentTurn.setTurnId("parent");
      parentTurn.setReply(parentReply);

      // Subagent turn persisted with parentTurnId + subagentToolCallId.
      ReplyData subReply = new ReplyData();
      subReply.setText("Subagent result.");
      CopilotTurnData subTurn = new CopilotTurnData();
      subTurn.setTurnId("sub");
      subTurn.setParentTurnId("parent");
      subTurn.setSubagentToolCallId("sub-tc");
      subTurn.setReply(subReply);

      // Parent precedes subagent in persisted order (as ChatView restores them).
      assertDoesNotThrow(() -> {
        widget.restoreTurn(parentTurn, dataFactory);
        widget.restoreTurn(subTurn, dataFactory);
      });
    }

    private ChatProgressValue toolCallReport(String turnId, String parentTurnId,
        String toolCallId, String toolName, String status) {
      AgentToolCall toolCall = new AgentToolCall();
      setField(toolCall, "id", toolCallId);
      setField(toolCall, "name", toolName);
      setField(toolCall, "status", status);
      AgentRound round = new AgentRound();
      setField(round, "toolCalls", List.of(toolCall));
      ChatProgressValue event = baseReport(turnId, parentTurnId);
      setField(event, "editAgentRounds", List.of(round));
      return event;
    }

    private ChatProgressValue thinkingReport(String turnId, String parentTurnId, String text) {
      ChatProgressValue event = baseReport(turnId, parentTurnId);
      event.setThinking(new Thinking("t", text, null));
      return event;
    }

    private ChatProgressValue baseReport(String turnId, String parentTurnId) {
      ChatProgressValue event = new ChatProgressValue();
      event.setKind(WorkDoneProgressKind.report);
      event.setTurnId(turnId);
      if (parentTurnId != null) {
        event.setParentTurnId(parentTurnId);
      }
      return event;
    }

    private ChatProgressValue endEvent(String turnId, String parentTurnId) {
      ChatProgressValue event = new ChatProgressValue();
      event.setKind(WorkDoneProgressKind.end);
      event.setTurnId(turnId);
      if (parentTurnId != null) {
        event.setParentTurnId(parentTurnId);
      }
      return event;
    }
  }

  /** Sets a private field via reflection (used for fields without public setters). */
  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set field " + fieldName, e);
    }
  }

  /** Runs an action on the SWT UI thread; SWT widget disposal must happen there. */
  private static void runOnUiThread(Runnable action) {
    Display.getDefault().syncExec(action);
  }
}

