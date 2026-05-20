// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentRound;
import com.microsoft.copilot.eclipse.core.lsp.protocol.AgentToolCall;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCompletionContentPart;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationError;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Thinking;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.EditAgentRoundData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ErrorMessageData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ReplyData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockData;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ThinkingBlockState;
import com.microsoft.copilot.eclipse.core.persistence.CopilotTurnData.ToolCallData;
import com.microsoft.copilot.eclipse.core.persistence.UserTurnData.MessageData;

/**
 * Factory for creating and transforming conversation data objects. Responsible only for pure data transformation with
 * no business logic.
 */
public class ConversationDataFactory {
  private static final int SYNTHETIC_ROUND_ID = -1;

  private final AuthStatusManager authStatusManager;

  /**
   * Constructor for ConversationDataFactory.
   *
   * @param authStatusManager the authentication status manager
   */
  public ConversationDataFactory(AuthStatusManager authStatusManager) {
    this.authStatusManager = authStatusManager;
  }

  /**
   * Creates a new ConversationData from initial parameters.
   */
  public ConversationData createConversationData(String conversationId) {
    ConversationData conversationData = new ConversationData();
    conversationData.setConversationId(conversationId);
    conversationData.setRequesterUsername(authStatusManager.getUserName());
    conversationData.setCreationDate(Instant.now());
    conversationData.setLastMessageDate(Instant.now());

    return conversationData;
  }

  /**
   * Creates a user turn from a message.
   */
  public UserTurnData createUserTurnData(String conversationId, String turnId, String message, String model,
      String chatMode, String customChatModeId) {
    UserTurnData userTurn = new UserTurnData();
    userTurn.setTurnId(turnId);
    userTurn.setMessage(new MessageData(message));
    userTurn.setReferences(new ArrayList<>());
    userTurn.setIgnoredSkills(new ArrayList<>());
    userTurn.setUserLanguage("en");
    userTurn.setSource("panel");
    userTurn.setNeedToolCallConfirmation(true);
    userTurn.setTimestamp(Instant.now());
    userTurn.setModel(model);
    userTurn.setChatMode(chatMode);
    userTurn.setCustomChatModeId(customChatModeId);

    return userTurn;
  }

  /**
   * Creates a copilot turn data for assistant responses.
   */
  public CopilotTurnData createCopilotTurnData(String turnId) {
    CopilotTurnData copilotTurn = new CopilotTurnData();
    copilotTurn.setRole("copilot");
    copilotTurn.setTurnId(turnId);
    copilotTurn.setTimestamp(Instant.now());
    copilotTurn.setReply(new ReplyData());

    return copilotTurn;
  }

  /**
   * Updates reply data from progress value (pure transformation).
   *
   * @param reply the reply data to update
   * @param progress the progress value to extract data from
   * @param thinkingBlockId the UI-generated thinking block ID, when the progress belongs to a thinking round
   */
  public void updateReplyFromProgress(ReplyData reply, ChatProgressValue progress, String thinkingBlockId) {
    ensureReplyInitialized(reply);

    // Accumulate thinking content
    appendThinkingContent(reply, progress.getThinking(), thinkingBlockId);

    // Merge agent rounds and append tool calls
    mergeAgentRounds(reply, progress.getAgentRounds(), thinkingBlockId);

    // Handle plain reply streaming (no agentRounds)
    appendProgressReplyText(reply, progress.getReply());

    // Apply error if any
    applyConversationError(reply, progress.getConversationError());

    // Append arrays into lists if present
    addAllIfPresent(reply.getAnnotations(), progress.getAnnotations());
    addAllIfPresent(reply.getReferences(), progress.getReferences());
    addAllIfPresent(reply.getSteps(), progress.getSteps());
    addAllIfPresent(reply.getNotifications(), progress.getNotifications());

    reply.setHideText(progress.isHideText());
  }

  private void mergeAgentRounds(ReplyData reply, List<AgentRound> agentRounds, String thinkingBlockId) {
    if (agentRounds == null || agentRounds.isEmpty()) {
      return;
    }
    for (AgentRound round : agentRounds) {
      EditAgentRoundData existingRound = findRoundById(reply.getEditAgentRounds(), round.getRoundId());
      if (existingRound == null) {
        existingRound = findThinkingPlaceholderRound(reply.getEditAgentRounds(), thinkingBlockId);
      }
      if (existingRound == null) {
        EditAgentRoundData er = convertAgentRoundToEditAgentRoundData(round);
        reply.getEditAgentRounds().add(er);
      } else {
        updateAgentRoundData(existingRound, round);
      }
    }
  }

  private void appendProgressReplyText(ReplyData replyData, String replyText) {
    if (StringUtils.isBlank(replyText)) {
      return;
    }
    List<EditAgentRoundData> rounds = replyData.getEditAgentRounds();

    if (rounds.isEmpty()) {
      // Chat mode reply message append to the main reply text
      String existingReplyText = StringUtils.isBlank(replyData.getText()) ? "" : replyData.getText();
      replyData.setText(existingReplyText + replyText);
    } else {
      // Agent mode reply message append to the last round's reply
      EditAgentRoundData existingLastRound = rounds.get(rounds.size() - 1);
      appendReplyToAgentRound(existingLastRound, replyText);
    }
  }

  // Generic helper to add array elements to a list if the array is present and non-empty
  private <T> void addAllIfPresent(List<T> target, T[] items) {
    if (target == null || items == null || items.length == 0) {
      return;
    }
    target.addAll(Arrays.asList(items));
  }

  private void appendReplyToAgentRound(EditAgentRoundData round, String addition) {
    String existingReply = StringUtils.isBlank(round.getReply()) ? "" : round.getReply();
    round.setReply(existingReply + addition);
  }

  private void appendThinkingContent(ReplyData reply, Thinking thinking, String thinkingBlockId) {
    // Preserve whitespace-only thinking fragments; they can carry markdown boundaries between sections.
    if (thinking == null || StringUtils.isEmpty(thinking.text())) {
      return;
    }
    if (StringUtils.isBlank(thinkingBlockId)) {
      return;
    }
    EditAgentRoundData round = getOrCreateThinkingRound(reply, thinkingBlockId);
    ThinkingBlockData block = round.getThinkingBlock();
    block.setContent(StringUtils.defaultString(block.getContent()) + thinking.text());
  }

  private void applyConversationError(ReplyData reply, ConversationError error) {
    if (error == null) {
      return;
    }
    ErrorData errorData = new ErrorData();
    errorData.setMessage(error.getMessage());
    errorData.setCode(error.getCode());
    errorData.setModelProviderName(error.getModelProviderName());
    ErrorMessageData em = new ErrorMessageData();
    em.setError(errorData);
    reply.getErrorMessages().add(em);
  }

  /**
   * Updates basic conversation metadata from progress (pure transformation).
   */
  public void updateConversationMetadata(ConversationData conversationData, ChatProgressValue progress) {
    if (StringUtils.isNotBlank(progress.getSuggestedTitle())) {
      conversationData.setTitle(progress.getSuggestedTitle());
    }
    conversationData.setLastMessageDate(Instant.now());
  }

  /**
   * Converts a list of persisted TurnData into protocol Turn objects for LSP communication.
   *
   * @param turnDataList the list of turn data to convert
   * @return list of Turn objects for protocol use
   */
  public List<Turn> convertToTurns(List<AbstractTurnData> turnDataList) {
    if (turnDataList == null || turnDataList.isEmpty()) {
      return new ArrayList<>();
    }
    // Defensive copy to avoid ConcurrentModificationException if another thread mutates the list while iterating.
    List<AbstractTurnData> snapshot = new ArrayList<>(turnDataList);
    List<Turn> result = new ArrayList<>(snapshot.size());
    Turn unpairedUserTurn = null;

    for (AbstractTurnData turnData : snapshot) {
      if (turnData == null) {
        continue;
      }
      // Skip subagent turns - they are not part of the main conversation history
      if (turnData instanceof CopilotTurnData copilotCheck
          && copilotCheck.getParentTurnId() != null) {
        continue;
      }
      if (turnData instanceof UserTurnData userTurnData) {
        // Flush any unpaired user turn without a response
        if (unpairedUserTurn != null) {
          result.add(unpairedUserTurn);
        }
        String requestText = userTurnData.getMessage() != null ? userTurnData.getMessage().getText() : "";
        Either<String, List<ChatCompletionContentPart>> request = Either
            .forLeft(requestText == null ? "" : requestText);
        unpairedUserTurn = new Turn(request, null, null, turnData.getTurnId());
      } else if (turnData instanceof CopilotTurnData copilotTurnData) {
        String responseText = extractResponseFromCopilotTurnData(copilotTurnData);
        if (unpairedUserTurn != null) {
          // Pair the response with the unpaired user turn
          unpairedUserTurn.setResponse(responseText);
          result.add(unpairedUserTurn);
          unpairedUserTurn = null;
        } else {
          // Orphaned copilot turn (no preceding user turn), create a standalone turn
          result.add(new Turn(Either.forLeft(""), responseText, null, turnData.getTurnId()));
        }
      }
    }
    // Flush any remaining unpaired user turn (user message without a response)
    if (unpairedUserTurn != null) {
      result.add(unpairedUserTurn);
    }
    return result;
  }

  /**
   * Extracts a single response string from AbstractTurnData by concatenating non-blank edit agent round replies. Only
   * works for CopilotTurnData instances that contain reply data.
   *
   * @param turnData the turn data to extract response from
   * @return concatenated response string, or null if no response
   */
  private String extractResponseFromCopilotTurnData(CopilotTurnData copilotTurnData) {
    ReplyData reply = copilotTurnData.getReply();
    if (reply == null) {
      return "";
    }

    List<EditAgentRoundData> rounds = reply.getEditAgentRounds();
    if (rounds == null || rounds.isEmpty()) {
      // If no agent rounds, check for plain text reply
      return StringUtils.isBlank(reply.getText()) ? "" : reply.getText();
    }

    StringBuilder sb = new StringBuilder();
    for (EditAgentRoundData round : rounds) {
      if (round != null && round.getReply() != null) {
        sb.append(round.getReply());
      }
    }
    String response = sb.toString();
    return response.isBlank() ? "" : response;
  }

  // Private helper methods for data transformation
  private EditAgentRoundData getOrCreateThinkingRound(ReplyData reply, String thinkingBlockId) {
    EditAgentRoundData existingRound = findRoundByThinkingBlockId(reply.getEditAgentRounds(), thinkingBlockId);
    if (existingRound != null) {
      return existingRound;
    }
    EditAgentRoundData round = new EditAgentRoundData();
    round.setRoundId(SYNTHETIC_ROUND_ID);
    round.setToolCalls(new ArrayList<>());
    round.setThinkingBlock(new ThinkingBlockData(thinkingBlockId, ""));
    reply.getEditAgentRounds().add(round);
    return round;
  }

  private void updateAgentRoundData(EditAgentRoundData target, AgentRound source) {
    target.setRoundId(source.getRoundId());
    appendReplyToAgentRound(target, source.getReply());
    ThinkingBlockData thinkingBlock = target.getThinkingBlock();
    if (thinkingBlock != null && !thinkingBlock.isFinalized()) {
      thinkingBlock.setState(ThinkingBlockState.COMPLETED);
    }
    if (source.getToolCalls() == null || source.getToolCalls().isEmpty()) {
      return;
    }
    if (target.getToolCalls() == null) {
      target.setToolCalls(new ArrayList<>());
    }
    for (AgentToolCall toolCall : source.getToolCalls()) {
      ToolCallData existingToolCall = findToolCallById(target.getToolCalls(), toolCall.getId());
      if (existingToolCall == null) {
        target.getToolCalls().add(convertAgentToolCallToToolCallData(toolCall));
      } else {
        updateToolCallData(existingToolCall, toolCall);
      }
    }
  }

  private EditAgentRoundData findRoundByThinkingBlockId(List<EditAgentRoundData> rounds, String thinkingBlockId) {
    if (rounds == null || StringUtils.isBlank(thinkingBlockId)) {
      return null;
    }
    for (EditAgentRoundData round : rounds) {
      ThinkingBlockData thinkingBlock = round.getThinkingBlock();
      if (thinkingBlock != null && thinkingBlockId.equals(thinkingBlock.getId())) {
        return round;
      }
    }
    return null;
  }

  private EditAgentRoundData findThinkingPlaceholderRound(List<EditAgentRoundData> rounds, String thinkingBlockId) {
    if (rounds == null || StringUtils.isBlank(thinkingBlockId)) {
      return null;
    }
    for (EditAgentRoundData round : rounds) {
      ThinkingBlockData thinkingBlock = round.getThinkingBlock();
      if (Objects.equals(round.getRoundId(), SYNTHETIC_ROUND_ID) && thinkingBlock != null
          && thinkingBlockId.equals(thinkingBlock.getId())) {
        return round;
      }
    }
    return null;
  }

  private EditAgentRoundData findRoundById(List<EditAgentRoundData> rounds, int roundId) {
    if (rounds == null) {
      return null;
    }
    for (EditAgentRoundData round : rounds) {
      if (round.getRoundId() == roundId) {
        return round;
      }
    }
    return null;
  }

  /**
   * Converts an AgentRound to EditAgentRoundData.
   */
  private EditAgentRoundData convertAgentRoundToEditAgentRoundData(AgentRound agentRound) {
    EditAgentRoundData roundData = new EditAgentRoundData();
    roundData.setRoundId(agentRound.getRoundId());
    roundData.setReply(agentRound.getReply());

    List<ToolCallData> toolCallsList = new ArrayList<>();
    if (agentRound.getToolCalls() != null && !agentRound.getToolCalls().isEmpty()) {
      for (AgentToolCall agentToolCall : agentRound.getToolCalls()) {
        ToolCallData toolCallData = convertAgentToolCallToToolCallData(agentToolCall);
        toolCallsList.add(toolCallData);
      }
    }
    roundData.setToolCalls(toolCallsList);

    return roundData;
  }

  /**
   * Converts an AgentToolCall to ToolCallData.
   */
  private ToolCallData convertAgentToolCallToToolCallData(AgentToolCall agentToolCall) {
    ToolCallData toolCallData = new ToolCallData();

    // Extract properties from AgentToolCall (avoid defaulting to empty strings to preserve null vs empty)
    if (agentToolCall.getId() != null) {
      toolCallData.setId(agentToolCall.getId());
    }
    if (agentToolCall.getName() != null) {
      toolCallData.setName(agentToolCall.getName());
    }
    if (agentToolCall.getProgressMessage() != null) {
      toolCallData.setProgressMessage(agentToolCall.getProgressMessage());
    }
    if (agentToolCall.getStatus() != null) {
      toolCallData.setStatus(agentToolCall.getStatus());
    }

    // Handle error - if there's an error, add it to the result; otherwise initialize empty result list
    if (StringUtils.isNotBlank(agentToolCall.getError())) {
      applyToolErrorResult(toolCallData, agentToolCall.getError());
    } else {
      toolCallData.setResult(new ArrayList<>());
    }

    return toolCallData;
  }

  private void applyToolErrorResult(ToolCallData toolCallData, String error) {
    Map<String, Object> errorResult = new HashMap<>();
    errorResult.put("error", error);
    toolCallData.setResult(List.of(errorResult));
  }

  private ToolCallData findToolCallById(List<ToolCallData> toolCalls, String id) {
    if (toolCalls == null || id == null) {
      return null;
    }

    for (ToolCallData toolCall : toolCalls) {
      if (id.equals(toolCall.getId())) {
        return toolCall;
      }
    }
    return null;
  }

  private void updateToolCallData(ToolCallData target, AgentToolCall source) {
    if (target == null || source == null) {
      return;
    }

    // Update fields as necessary
    target.setName(source.getName() != null ? source.getName() : target.getName());
    target.setProgressMessage(
        source.getProgressMessage() != null ? source.getProgressMessage() : target.getProgressMessage());
    target.setStatus(source.getStatus() != null ? source.getStatus() : target.getStatus());

    // Update result - replace with error or reset to empty list
    if (source.getError() != null && !source.getError().trim().isEmpty()) {
      applyToolErrorResult(target, source.getError());
    } else {
      target.setResult(new ArrayList<>());
    }
  }

  // Ensures ReplyData and all of its lists are initialized to avoid repeated null checks
  private void ensureReplyInitialized(ReplyData reply) {
    if (reply.getAnnotations() == null) {
      reply.setAnnotations(new ArrayList<>());
    }
    if (reply.getReferences() == null) {
      reply.setReferences(new ArrayList<>());
    }
    if (reply.getNotifications() == null) {
      reply.setNotifications(new ArrayList<>());
    }
    if (reply.getFollowups() == null) {
      reply.setFollowups(new ArrayList<>());
    }
    if (reply.getErrorMessages() == null) {
      reply.setErrorMessages(new ArrayList<>());
    }
    if (reply.getEditAgentRounds() == null) {
      reply.setEditAgentRounds(new ArrayList<>());
    }
    if (reply.getPanelMessages() == null) {
      reply.setPanelMessages(new ArrayList<>());
    }
    if (reply.getSteps() == null) {
      reply.setSteps(new ArrayList<>());
    }
  }

  /**
   * Converts persisted ToolCallData to AgentToolCall for use with UI components. This is the reverse operation of
   * convertAgentToolCallToToolCallData.
   *
   * @param toolCallData the persisted tool call data
   * @return AgentToolCall instance for use with UI components
   */
  public AgentToolCall convertToolCallDataToAgentToolCall(ToolCallData toolCallData) {
    if (toolCallData == null) {
      return null;
    }

    return new AgentToolCall() {
      @Override
      public String getId() {
        return toolCallData.getId();
      }

      @Override
      public String getName() {
        return toolCallData.getName();
      }

      @Override
      public String getProgressMessage() {
        return toolCallData.getProgressMessage();
      }

      @Override
      public String getStatus() {
        return toolCallData.getStatus();
      }

      @Override
      public String getError() {
        // Extract error from result if present
        if (toolCallData.getResult() != null && !toolCallData.getResult().isEmpty()) {
          for (Map<String, Object> result : toolCallData.getResult()) {
            if (result.containsKey("error")) {
              return result.get("error").toString();
            }
          }
        }
        return null;
      }
    };
  }
}