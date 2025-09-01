package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IResource;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Turn data that defines the Turn JSON schema for persistence (updated schema).
 */
public class TurnData {
  private String turnId;
  private String conversationId;
  private String role;
  private MessageData message;
  private TextDocument currentDocument;
  private List<TextDocument> references;
  private List<Object> ignoredSkills;
  private String userLanguage;
  private String source;
  private String model;
  private String chatMode;
  private boolean needToolCallConfirmation;
  private Instant timestamp;
  private ReplyData reply;
  private Map<String, Object> data;

  /**
   * Default constructor initializing default values.
   */
  public TurnData() {
    this.needToolCallConfirmation = true;
    this.references = new ArrayList<>();
  }

  public String getTurnId() {
    return turnId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public String getConversationId() {
    return conversationId;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public MessageData getMessage() {
    return message;
  }

  public void setMessage(MessageData message) {
    this.message = message;
  }

  public TextDocument getCurrentDocument() {
    return currentDocument;
  }

  /**
   * Sets the current document from an IResource.
   *
   * @param currentDocument The IResource representing the text document.
   */
  public void setCurrentDocument(IResource currentDocument) {
    if (currentDocument == null) {
      return;
    }
    this.currentDocument = new TextDocument(currentDocument);
  }

  public List<TextDocument> getReferences() {
    return references;
  }

  /**
   * Sets the references from a list of IResource objects.
   *
   * @param references The list of IResource representing the references.
   */
  public void setReferences(List<IResource> references) {
    if (references == null) {
      return;
    }
    references.stream().forEach(ref -> {
      if (ref != null) {
        this.references.add(new TextDocument(ref));
      }
    });
  }

  public List<Object> getIgnoredSkills() {
    return ignoredSkills;
  }

  public void setIgnoredSkills(List<Object> ignoredSkills) {
    this.ignoredSkills = ignoredSkills;
  }

  public String getUserLanguage() {
    return userLanguage;
  }

  public void setUserLanguage(String userLanguage) {
    this.userLanguage = userLanguage;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getChatMode() {
    return chatMode;
  }

  public void setChatMode(String chatMode) {
    this.chatMode = chatMode;
  }

  public boolean isNeedToolCallConfirmation() {
    return needToolCallConfirmation;
  }

  public void setNeedToolCallConfirmation(boolean needToolCallConfirmation) {
    this.needToolCallConfirmation = needToolCallConfirmation;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public ReplyData getReply() {
    return reply;
  }

  public void setReply(ReplyData reply) {
    this.reply = reply;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  /**
   * Message data that defines the structure of user messages in the turn data.
   */
  public static class MessageData {
    private String text;

    /**
     * Default constructor initializing an empty message.
     */
    public MessageData(String messageText) {
      this.text = messageText;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    @Override
    public int hashCode() {
      return Objects.hash(text);
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
      MessageData other = (MessageData) obj;
      return Objects.equals(text, other.text);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("text", text);
      return builder.toString();
    }

  }

  /**
   * Reply data that defines the structure of Copilot replies in the turn data.
   */
  public static class ReplyData {
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
     * Constructor for ReplyData initializes all lists to avoid null checks.
     */
    public ReplyData() {
      this.hideText = false;
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
          panelMessages, rating, references, steps);
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
          && Objects.equals(references, other.references) && Objects.equals(steps, other.steps);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
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
   * Error message data that defines the structure of error messages in the turn data.
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
   * Error data that defines the structure of error messages in the turn data.
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
   * Edit agent round data that defines the structure of edit agent rounds in the turn data.
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
   * Tool call data that defines the structure of tool calls in the turn data.
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

  /**
   * Text document data that defines current file in this turn.
   */
  public static class TextDocument {
    private String uri;

    /**
     * Default constructor initializing the TextDocument with the file's URI.
     */
    public TextDocument(IResource file) {
      this.uri = file.getFullPath().toString();
    }

    public String getUri() {
      return uri;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public int hashCode() {
      return Objects.hash(uri);
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
      TextDocument other = (TextDocument) obj;
      return Objects.equals(uri, other.uri);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("uri", uri);
      return builder.toString();
    }
  }
}