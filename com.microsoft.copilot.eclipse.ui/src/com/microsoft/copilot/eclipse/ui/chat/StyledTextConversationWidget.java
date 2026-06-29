// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolConfirmationResult.ToolConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.codingagent.CodingAgentMessageRequestParams;
import com.microsoft.copilot.eclipse.core.persistence.AbstractTurnData;
import com.microsoft.copilot.eclipse.core.persistence.ConversationDataFactory;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.AgentMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * {@link IConversationWidget} implementation backed by the existing SWT {@link ChatContentViewer}
 * using {@link StyledText} to render the content.
 * This is a thin adapter that delegates all calls to the underlying viewer.
 */
public class StyledTextConversationWidget implements IConversationWidget {

  private final ChatContentViewer viewer;

  /**
   * Creates a new {@link StyledTextConversationWidget} that internally creates a {@link ChatContentViewer}.
   *
   * @param parent the parent composite
   * @param serviceManager the chat service manager
   */
  public StyledTextConversationWidget(Composite parent, ChatServiceManager serviceManager) {
    this.viewer = new ChatContentViewer(parent, SWT.NONE, serviceManager);
  }

  /**
   * Returns the underlying {@link ChatContentViewer} for SWT-specific operations that are not part
   * of the {@link IConversationWidget} interface (e.g., {@code getTurnWidget()}).
   */
  public ChatContentViewer getChatContentViewer() {
    return viewer;
  }

  @Override
  public Control getControl() {
    return viewer;
  }

  @Override
  public boolean isDisposed() {
    return viewer.isDisposed();
  }

  @Override
  public void dispose() {
    viewer.dispose();
  }

  @Override
  public void requestLayout() {
    viewer.requestLayout();
  }

  @Override
  public void setConversationId(String conversationId) {
    viewer.setConversationId(conversationId);
  }

  @Override
  public void beginTurn(String turnId, boolean isCopilot, boolean isHistory) {
    viewer.getLatestOrCreateNewTurnWidget(turnId, isCopilot, isHistory);
  }

  @Override
  public void processTurnEvent(ChatProgressValue value) {
    viewer.processTurnEvent(value);
  }

  @Override
  public void startNewUserTurn(String turnId, String message) {
    viewer.startNewTurn(turnId, message);
  }

  @Override
  public void scrollToBottom() {
    viewer.getDisplay().asyncExec(() -> {
      if (viewer.isDisposed()) {
        return;
      }
      viewer.refreshLayoutFull();
      viewer.scrollToBottomIfAutoScroll();
    });
  }

  @Override
  public void refreshScrollerLayout() {
    viewer.refreshLayoutFull();
  }

  @Override
  public void renderErrorMessage(String content) {
    viewer.renderErrorMessage(content);
  }

  @Override
  public void showCompactingStatusOnLatestCopilotTurn() {
    viewer.showCompactingStatusOnLatestCopilotTurn();
  }

  @Override
  public void hideCompactingStatusOnLatestCopilotTurn() {
    viewer.hideCompactingStatusOnLatestCopilotTurn();
  }

  @Override
  public String getActiveThinkingBlockId(String turnId) {
    return viewer.getActiveThinkingBlockId(turnId);
  }

  @Override
  public void restoreTurn(AbstractTurnData turn, ConversationDataFactory dataFactory) {
    if (turn == null) {
      return;
    }

    // Subagent turns: render inside parent turn's subagent block
    if (turn instanceof CopilotTurnData copilotTurn
        && StringUtils.isNotBlank(copilotTurn.getParentTurnId())) {
      BaseTurnWidget parentWidget = viewer.getTurnWidget(copilotTurn.getParentTurnId());
      if (parentWidget != null) {
        String toolCallId = copilotTurn.getSubagentToolCallId();
        if (StringUtils.isNotBlank(toolCallId)) {
          parentWidget.restoreSubagentContent(toolCallId, copilotTurn, dataFactory);
        } else {
          restoreCopilotTurnContent(copilotTurn, parentWidget, dataFactory);
        }
      }
      return;
    }

    // User turn
    if (turn instanceof UserTurnData userTurn) {
      if (userTurn.getMessage() == null
          || StringUtils.isNotBlank(userTurn.getMessage().getText())) {
        BaseTurnWidget userTurnWidget =
            viewer.getLatestOrCreateNewTurnWidget(turn.getTurnId(), false, true);
        userTurnWidget.appendMessage(userTurn.getMessage().getText());
        userTurnWidget.flushMessageBuffer();
      }
      return;
    }

    // Copilot turn
    if (turn instanceof CopilotTurnData copilotTurn) {
      BaseTurnWidget copilotTurnWidget =
          viewer.getLatestOrCreateNewTurnWidget(turn.getTurnId(), true, true);
      restoreCopilotTurnContent(copilotTurn, copilotTurnWidget, dataFactory);
      copilotTurnWidget.flushMessageBuffer();

      // Restore model info footer
      ReplyData replyData = copilotTurn.getReply();
      if (replyData != null && StringUtils.isNotBlank(replyData.getModelName())) {
        renderModelInfo(turn.getTurnId(), replyData.getModelName(),
            replyData.getBillingMultiplier(), replyData.getReasoningEffort());
      }
    }
  }

  @Override
  public void renderModelInfo(String turnId, String modelName, double billingMultiplier,
      String reasoningEffort) {
    if (viewer.isDisposed()) {
      return;
    }
    BaseTurnWidget turnWidget = viewer.getTurnWidget(turnId);
    if (turnWidget instanceof CopilotTurnWidget copilotWidget) {
      copilotWidget.renderModelInfo(modelName, billingMultiplier, reasoningEffort);
      SwtUtils.invokeOnDisplayThreadAsync(viewer::refreshLayoutFull, viewer);
    }
  }

  @Override
  public void renderAgentMessage(CodingAgentMessageRequestParams params) {
    if (viewer.isDisposed() || params == null) {
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      BaseTurnWidget turnWidget = viewer.getTurnWidget(params.getTurnId());
      if (turnWidget != null && !turnWidget.isDisposed()) {
        turnWidget.createAgentMessageWidget(params);
      }
    }, viewer);
  }

  @Override
  public CompletableFuture<LanguageModelToolConfirmationResult> requestToolConfirmation(
      String turnId, ConfirmationContent content, Object input) {
    BaseTurnWidget turnWidget = viewer.getTurnWidget(turnId);
    if (turnWidget == null) {
      return CompletableFuture.completedFuture(
          new LanguageModelToolConfirmationResult(ToolConfirmationResult.DISMISS));
    }
    BaseTurnWidget activeTurnWidget = turnWidget.getActiveTurnWidget();
    CompletableFuture<LanguageModelToolConfirmationResult> future =
        activeTurnWidget.requestToolExecutionConfirmation(content, input);
    viewer.refreshLayoutFull();
    return future;
  }

  @Override
  public void cancelToolConfirmation(String turnId) {
    BaseTurnWidget turnWidget = viewer.getTurnWidget(turnId);
    if (turnWidget != null) {
      turnWidget.getActiveTurnWidget().cancelToolConfirmation();
    }
  }

  @Override
  public ConfirmationAction getLastSelectedConfirmationAction() {
    // In the SWT implementation, the selected action is retrieved from the dialog directly
    // by AgentToolService. Return null here; SWT path uses dialog.getSelectedAction().
    return null;
  }

  private void restoreCopilotTurnContent(CopilotTurnData copilotTurn, BaseTurnWidget turnWidget,
      ConversationDataFactory dataFactory) {
    ReplyData replyData = copilotTurn.getReply();
    if (replyData == null) {
      return;
    }

    ThinkingTurnWidget thinkingWidget =
        turnWidget instanceof ThinkingTurnWidget ? (ThinkingTurnWidget) turnWidget : null;

    if (StringUtils.isNotBlank(replyData.getText())) {
      turnWidget.appendMessage(replyData.getText());
    }

    if (replyData.getEditAgentRounds() != null && !replyData.getEditAgentRounds().isEmpty()) {
      for (EditAgentRoundData round : replyData.getEditAgentRounds()) {
        if (thinkingWidget != null && round.getThinkingBlock() != null) {
          thinkingWidget.restoreThinkingBlock(round.getThinkingBlock());
        }
        if (round.getReply() != null && !round.getReply().isEmpty()) {
          turnWidget.appendMessage(round.getReply());
        }
        if (round.getToolCalls() != null && !round.getToolCalls().isEmpty()) {
          for (ToolCallData toolCallData : round.getToolCalls()) {
            AgentToolCall agentToolCall =
                dataFactory.convertToolCallDataToAgentToolCall(toolCallData);
            turnWidget.appendToolCallStatus(agentToolCall);
          }
        }
      }
    }

    // Flush buffered text before creating error/agent widgets so that reply text
    // always appears above them in the layout.
    turnWidget.flushMessageBuffer();

    if (replyData.getErrorMessages() != null && !replyData.getErrorMessages().isEmpty()) {
      for (ErrorMessageData errorMessageData : replyData.getErrorMessages()) {
        ErrorData errorData = errorMessageData.getError();
        SwtUtils.invokeOnDisplayThread(() -> {
          String errorMessage = errorData != null
              ? errorData.getMessage() : Messages.chat_warnWidget_defaultErrorMsg;
          int errorCode = errorData != null ? errorData.getCode() : 0;
          String modelProviderName = errorData != null ? errorData.getModelProviderName() : null;
          turnWidget.createWarnDialog(errorMessage, errorCode, modelProviderName);
        }, viewer);
      }
    }

    if (replyData.getAgentMessages() != null && !replyData.getAgentMessages().isEmpty()) {
      for (AgentMessageData agentMessageData : replyData.getAgentMessages()) {
        if (StringUtils.equals(agentMessageData.getAgentSlug(),
            UiConstants.GITHUB_COPILOT_CODING_AGENT_SLUG)) {
          SwtUtils.invokeOnDisplayThread(() -> {
            CodingAgentMessageRequestParams params = new CodingAgentMessageRequestParams();
            params.setTitle(agentMessageData.getTitle());
            params.setDescription(agentMessageData.getDescription());
            params.setPrLink(agentMessageData.getPrLink());
            params.setTurnId(copilotTurn.getTurnId());
            turnWidget.createAgentMessageWidget(params);
          }, viewer);
        }
      }
    }
  }
}
