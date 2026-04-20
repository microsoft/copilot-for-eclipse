// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IResource;

/**
 * Represents a user turn (the request) in a conversation. Split out from the original TurnData. Contains only user
 * authored/request specific information.
 */
public class UserTurnData extends AbstractTurnData {
  private MessageData message;
  private TextDocument currentDocument;
  private List<TextDocument> references;
  private List<Object> ignoredSkills;
  private String userLanguage;
  private String source;
  private String model;
  private String chatMode;
  private String customChatModeId;
  private boolean needToolCallConfirmation;

  /**
   * Creates a new user turn initialized with default role "user" and empty collections for references and ignored
   * skills.
   */
  public UserTurnData() {
    this.role = "user";
    this.references = new ArrayList<>();
    this.ignoredSkills = new ArrayList<>();
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

  public void setCurrentDocument(TextDocument currentDocument) {
    this.currentDocument = currentDocument;
  }

  /**
   * Set the current document as a TextDocument instance created from the provided IResource.
   */
  public void setCurrentDocument(IResource resource) {
    if (resource != null) {
      this.currentDocument = new TextDocument(resource);
    }
  }

  public List<TextDocument> getReferences() {
    return references;
  }

  /**
   * Set the list of IResources as the references after converting them to TextDocument instances.
   */
  public void setReferences(List<IResource> refs) {
    if (refs != null) {
      refs.forEach(r -> {
        if (r != null) {
          this.references.add(new TextDocument(r));
        }
      });
    }
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

  public String getCustomChatModeId() {
    return customChatModeId;
  }

  public void setCustomChatModeId(String customChatModeId) {
    this.customChatModeId = customChatModeId;
  }

  public boolean isNeedToolCallConfirmation() {
    return needToolCallConfirmation;
  }

  public void setNeedToolCallConfirmation(boolean needToolCallConfirmation) {
    this.needToolCallConfirmation = needToolCallConfirmation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(chatMode, customChatModeId, currentDocument, ignoredSkills, message, model,
        needToolCallConfirmation, references, source, userLanguage);
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
    UserTurnData other = (UserTurnData) obj;
    return Objects.equals(chatMode, other.chatMode) && Objects.equals(customChatModeId, other.customChatModeId)
        && Objects.equals(currentDocument, other.currentDocument)
        && Objects.equals(ignoredSkills, other.ignoredSkills) && Objects.equals(message, other.message)
        && Objects.equals(model, other.model) && needToolCallConfirmation == other.needToolCallConfirmation
        && Objects.equals(references, other.references) && Objects.equals(source, other.source)
        && Objects.equals(userLanguage, other.userLanguage);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    // Include AbstractTurnData properties
    builder.append("turnId", getTurnId());
    builder.append("role", getRole());
    builder.append("timestamp", getTimestamp());
    builder.append("data", getData());
    // Include UserTurnData specific properties
    builder.append("message", message);
    builder.append("currentDocument", currentDocument);
    builder.append("references", references);
    builder.append("ignoredSkills", ignoredSkills);
    builder.append("userLanguage", userLanguage);
    builder.append("source", source);
    builder.append("model", model);
    builder.append("chatMode", chatMode);
    builder.append("customChatModeId", customChatModeId);
    builder.append("needToolCallConfirmation", needToolCallConfirmation);
    return builder.toString();
  }

  /**
   * Builder used to fluently create and configure {@link UserTurnData} instances. Each setter returns the same builder
   * enabling chained calls prior to {@link #build()}.
   */
  public static class Builder {
    /**
     * Builder for {@link UserTurnData}. Provides a fluent API to incrementally configure a {@code UserTurnData}
     * instance prior to use.
     */
    private final UserTurnData target = new UserTurnData();

    /**
     * Creates a new builder instance with an empty underlying {@link UserTurnData}.
     */
    public Builder() {
      // default constructor
    }

    /**
     * Sets the unique turn identifier.
     *
     * @param turnId turn id (may be null until assigned by server)
     * @return this builder
     */
    public Builder turnId(String turnId) {
      target.setTurnId(turnId);
      return this;
    }

    /**
     * Sets the role for this turn (normally "user").
     *
     * @param role role value
     * @return this builder
     */
    public Builder role(String role) {
      target.setRole(role);
      return this;
    }

    /**
     * Sets the message text creating a new MessageData wrapper.
     *
     * @param messageText raw message text
     * @return this builder
     */
    public Builder message(String messageText) {
      target.setMessage(new MessageData(messageText));
      return this;
    }

    /**
     * Sets the message object directly.
     *
     * @param messageData message data instance
     * @return this builder
     */
    public Builder message(MessageData messageData) {
      target.setMessage(messageData);
      return this;
    }

    /**
     * Sets the current active document for the turn.
     *
     * @param currentDocument Eclipse resource representing the file
     * @return this builder
     */
    public Builder currentDocument(IResource currentDocument) {
      target.setCurrentDocument(currentDocument);
      return this;
    }

    /**
     * Sets referenced documents contributing context.
     *
     * @param references list of Eclipse resources
     * @return this builder
     */
    public Builder references(List<IResource> references) {
      target.setReferences(references);
      return this;
    }

    /**
     * Sets skills to ignore during processing.
     *
     * @param ignoredSkills list of ignored skills
     * @return this builder
     */
    public Builder ignoredSkills(List<Object> ignoredSkills) {
      target.setIgnoredSkills(ignoredSkills);
      return this;
    }

    /**
     * Sets the user language.
     *
     * @param userLanguage language code
     * @return this builder
     */
    public Builder userLanguage(String userLanguage) {
      target.setUserLanguage(userLanguage);
      return this;
    }

    /**
     * Sets the source (e.g. panel, inline, etc.).
     *
     * @param source source label
     * @return this builder
     */
    public Builder source(String source) {
      target.setSource(source);
      return this;
    }

    /**
     * Sets the model name used for this turn.
     *
     * @param model model identifier
     * @return this builder
     */
    public Builder model(String model) {
      target.setModel(model);
      return this;
    }

    /**
     * Sets the chat mode identifier.
     *
     * @param chatMode chat mode
     * @return this builder
     */
    public Builder chatMode(String chatMode) {
      target.setChatMode(chatMode);
      return this;
    }

    /**
     * Sets the custom chat mode identifier.
     *
     * @param customChatModeId custom chat mode ID
     * @return this builder
     */
    public Builder customChatModeId(String customChatModeId) {
      target.setCustomChatModeId(customChatModeId);
      return this;
    }

    /**
     * Sets whether tool call confirmation is required.
     *
     * @param needToolCallConfirmation true to require confirmation
     * @return this builder
     */
    public Builder needToolCallConfirmation(boolean needToolCallConfirmation) {
      target.setNeedToolCallConfirmation(needToolCallConfirmation);
      return this;
    }

    /**
     * Sets the timestamp for when the turn was created.
     *
     * @param timestamp instant timestamp
     * @return this builder
     */
    public Builder timestamp(Instant timestamp) {
      target.setTimestamp(timestamp);
      return this;
    }

    /**
     * Attaches arbitrary metadata.
     *
     * @param data map of additional data
     * @return this builder
     */
    public Builder data(Map<String, Object> data) {
      target.setData(data);
      return this;
    }

    /**
     * Builds the immutable UserTurnData instance.
     *
     * @return configured UserTurnData
     */
    public UserTurnData build() {
      return target;
    }
  }

  /**
   * Simple value object encapsulating the raw textual content supplied by the user for a single turn. Equality is based
   * solely on the text value.
   */
  public static class MessageData {
    /**
     * The message text content supplied by the user.
     */
    private String text;

    /**
     * Constructs a new {@link MessageData} wrapper around raw user supplied text.
     *
     * @param text the raw message content (may be {@code null} or empty)
     */
    public MessageData(String text) {
      this.text = text;
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
      builder.append("text", text);
      return builder.toString();
    }

  }

  /**
   * Lightweight representation of an Eclipse text document referenced for context during a user turn. Stores only the
   * workspace URI path for serialization.
   */
  public static class TextDocument {
    /**
     * URI (workspace path) of the referenced Eclipse resource.
     */
    private String uri;

    /**
     * Creates a new {@link TextDocument} representation for the provided Eclipse resource.
     *
     * @param file the Eclipse resource backing this document (must not be {@code null})
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
      builder.append("uri", uri);
      return builder.toString();
    }

  }
}
