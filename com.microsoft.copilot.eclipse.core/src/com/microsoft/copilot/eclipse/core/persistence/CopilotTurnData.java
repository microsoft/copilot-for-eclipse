package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents the Copilot (assistant) side of a turn. Split from original TurnData. Contains only reply related
 * information.
 */
public class CopilotTurnData extends AbstractTurnData {
  private ReplyData reply;
  private String suggestedTitle;

  /**
   * Default constructor initializing default values.
   */
  public CopilotTurnData() {
    this.reply = new ReplyData();
  }

  public ReplyData getReply() {
    return reply;
  }

  public void setReply(ReplyData reply) {
    this.reply = reply;
  }

  public String getSuggestedTitle() {
    return suggestedTitle;
  }

  public void setSuggestedTitle(String suggestedTitle) {
    this.suggestedTitle = suggestedTitle;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(reply, suggestedTitle);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CopilotTurnData other = (CopilotTurnData) obj;
    return Objects.equals(reply, other.reply) && Objects.equals(suggestedTitle, other.suggestedTitle);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    // Include AbstractTurnData properties
    builder.add("turnId", getTurnId());
    builder.add("role", getRole());
    builder.add("timestamp", getTimestamp());
    builder.add("data", getData());
    // Include CopilotTurnData specific properties
    builder.add("reply", reply);
    builder.add("suggestedTitle", suggestedTitle);
    return builder.toString();
  }

  /**
   * Creates a new builder to build a CopilotTurnData instance.
   *
   * @return builder instance
   */
  public static class Builder {
    private final CopilotTurnData target = new CopilotTurnData();

    /**
     * Sets the unique identifier of this turn.
     *
     * @param turnId turn id
     * @return this builder
     */
    public Builder turnId(String turnId) {
      target.setTurnId(turnId);
      return this;
    }

    /**
     * Sets the role.
     *
     * @param role role value
     * @return this builder
     */
    public Builder role(String role) {
      target.setRole(role);
      return this;
    }

    /**
     * Sets the reply data object.
     *
     * @param reply reply data
     * @return this builder
     */
    public Builder reply(ReplyData reply) {
      target.setReply(reply);
      return this;
    }

    /**
     * Sets the timestamp when this reply was received.
     *
     * @param timestamp instant timestamp
     * @return this builder
     */
    public Builder timestamp(Instant timestamp) {
      target.setTimestamp(timestamp);
      return this;
    }

    /**
     * Sets the suggested title derived from this turn.
     *
     * @param suggestedTitle suggested title string
     * @return this builder
     */
    public Builder suggestedTitle(String suggestedTitle) {
      target.setSuggestedTitle(suggestedTitle);
      return this;
    }

    /**
     * Builds the configured CopilotTurnData instance.
     *
     * @return CopilotTurnData
     */
    public CopilotTurnData build() {
      return target;
    }
  }

  /**
   * Data class representing the reply details of a Copilot turn.
   */
  public static class ReplyData {
    private String text;
    private List<Object> annotations;
    private List<Object> references;
    private boolean hideText;
    private List<Object> notifications;
    private List<Object> followups;
    private List<ErrorMessageData> errorMessages;
    private List<EditAgentRoundData> editAgentRounds;
    private List<Object> panelMessages;
    private Integer rating;
    private List<Object> steps;
    private Map<String, Object> data;

    /**
     * Default constructor initializing lists and data maps.
     */
    public ReplyData() {
      this.annotations = new ArrayList<>();
      this.references = new ArrayList<>();
      this.notifications = new ArrayList<>();
      this.followups = new ArrayList<>();
      this.errorMessages = new ArrayList<>();
      this.editAgentRounds = new ArrayList<>();
      this.panelMessages = new ArrayList<>();
      this.steps = new ArrayList<>();
      this.data = new HashMap<>();
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public List<Object> getAnnotations() {
      return annotations;
    }

    public void setAnnotations(List<Object> annotations) {
      this.annotations = annotations;
    }

    public List<Object> getReferences() {
      return references;
    }

    public void setReferences(List<Object> references) {
      this.references = references;
    }

    public boolean isHideText() {
      return hideText;
    }

    public void setHideText(boolean hideText) {
      this.hideText = hideText;
    }

    public List<Object> getNotifications() {
      return notifications;
    }

    public void setNotifications(List<Object> notifications) {
      this.notifications = notifications;
    }

    public List<Object> getFollowups() {
      return followups;
    }

    public void setFollowups(List<Object> followups) {
      this.followups = followups;
    }

    public List<ErrorMessageData> getErrorMessages() {
      return errorMessages;
    }

    public void setErrorMessages(List<ErrorMessageData> errorMessages) {
      this.errorMessages = errorMessages;
    }

    public List<EditAgentRoundData> getEditAgentRounds() {
      return editAgentRounds;
    }

    public void setEditAgentRounds(List<EditAgentRoundData> editAgentRounds) {
      this.editAgentRounds = editAgentRounds;
    }

    public List<Object> getPanelMessages() {
      return panelMessages;
    }

    public void setPanelMessages(List<Object> panelMessages) {
      this.panelMessages = panelMessages;
    }

    public Integer getRating() {
      return rating;
    }

    public void setRating(Integer rating) {
      this.rating = rating;
    }

    public List<Object> getSteps() {
      return steps;
    }

    public void setSteps(List<Object> steps) {
      this.steps = steps;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(annotations, data, editAgentRounds, errorMessages, followups, hideText, notifications,
          panelMessages, rating, references, steps, text);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ReplyData other = (ReplyData) obj;
      return Objects.equals(annotations, other.annotations) && Objects.equals(data, other.data)
          && Objects.equals(editAgentRounds, other.editAgentRounds)
          && Objects.equals(errorMessages, other.errorMessages) && Objects.equals(followups, other.followups)
          && hideText == other.hideText && Objects.equals(notifications, other.notifications)
          && Objects.equals(panelMessages, other.panelMessages) && Objects.equals(rating, other.rating)
          && Objects.equals(references, other.references) && Objects.equals(steps, other.steps)
          && Objects.equals(text, other.text);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("text", text);
      builder.add("annotations", annotations);
      builder.add("references", references);
      builder.add("hideText", hideText);
      builder.add("notifications", notifications);
      builder.add("followups", followups);
      builder.add("errorMessages", errorMessages);
      builder.add("editAgentRounds", editAgentRounds);
      builder.add("panelMessages", panelMessages);
      builder.add("rating", rating);
      builder.add("steps", steps);
      builder.add("data", data);
      return builder.toString();
    }
  }

  /**
   * Data class representing an error message in the reply.
   */
  public static class ErrorMessageData {
    private ErrorData error;
    private Map<String, Object> data;

    public ErrorData getError() {
      return error;
    }

    public void setError(ErrorData error) {
      this.error = error;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data, error);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ErrorMessageData other = (ErrorMessageData) obj;
      return Objects.equals(data, other.data) && Objects.equals(error, other.error);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("error", error);
      builder.add("data", data);
      return builder.toString();
    }
  }

  /**
   * Data class representing the details of an error.
   */
  public static class ErrorData {
    private String message;
    private int code;
    private Map<String, Object> data;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public int getCode() {
      return code;
    }

    public void setCode(int code) {
      this.code = code;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(code, data, message);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ErrorData other = (ErrorData) obj;
      return code == other.code && Objects.equals(data, other.data) && Objects.equals(message, other.message);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("message", message);
      builder.add("code", code);
      builder.add("data", data);
      return builder.toString();
    }
  }

  /**
   * Data class representing a round of an edit agent's activity.
   */
  public static class EditAgentRoundData {
    private int roundId;
    private String reply;
    private List<ToolCallData> toolCalls;
    private Map<String, Object> data;

    public int getRoundId() {
      return roundId;
    }

    public void setRoundId(int roundId) {
      this.roundId = roundId;
    }

    public String getReply() {
      return reply;
    }

    public void setReply(String reply) {
      this.reply = reply;
    }

    public List<ToolCallData> getToolCalls() {
      return toolCalls;
    }

    public void setToolCalls(List<ToolCallData> toolCalls) {
      this.toolCalls = toolCalls;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data, reply, roundId, toolCalls);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      EditAgentRoundData other = (EditAgentRoundData) obj;
      return Objects.equals(data, other.data) && Objects.equals(reply, other.reply) && roundId == other.roundId
          && Objects.equals(toolCalls, other.toolCalls);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("roundId", roundId);
      builder.add("reply", reply);
      builder.add("toolCalls", toolCalls);
      builder.add("data", data);
      return builder.toString();
    }
  }

  /**
   * Data class representing a tool call made by Copilot agent.
   */
  public static class ToolCallData {
    private String id;
    private String name;
    private String progressMessage;
    private String status;
    private List<Map<String, Object>> result;
    private Map<String, Object> data;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getProgressMessage() {
      return progressMessage;
    }

    public void setProgressMessage(String progressMessage) {
      this.progressMessage = progressMessage;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public List<Map<String, Object>> getResult() {
      return result;
    }

    public void setResult(List<Map<String, Object>> result) {
      this.result = result;
    }

    public Map<String, Object> getData() {
      return data;
    }

    public void setData(Map<String, Object> data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data, id, name, progressMessage, result, status);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ToolCallData other = (ToolCallData) obj;
      return Objects.equals(data, other.data) && Objects.equals(id, other.id) && Objects.equals(name, other.name)
          && Objects.equals(progressMessage, other.progressMessage) && Objects.equals(result, other.result)
          && Objects.equals(status, other.status);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("id", id);
      builder.add("name", name);
      builder.add("progressMessage", progressMessage);
      builder.add("status", status);
      builder.add("result", result);
      builder.add("data", data);
      return builder.toString();
    }
  }
}