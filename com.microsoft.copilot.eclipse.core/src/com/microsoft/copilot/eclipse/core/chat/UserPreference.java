package com.microsoft.copilot.eclipse.core.chat;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Preferences per GitHub user. All the getters and setters are synchronized due to that the ChatBaseService holds a
 * shared (single) reference to the user preference. synchronized modifies makes sure the update to the instance are
 * thread safe.
 */
public class UserPreference {

  private String modelName;
  private String chatModeName;

  /**
   * Gets the name of the AI model.
   *
   * @return the model name
   */
  public synchronized String getModelName() {
    return modelName;
  }

  /**
   * Sets the name of the AI model.
   *
   * @param modelName the model name
   */
  public synchronized void setModelName(String modelName) {
    this.modelName = modelName;
  }

  /**
   * Gets the name of the chat mode.
   *
   * @return the chat mode name
   */
  public synchronized String getChatModeName() {
    return chatModeName;
  }

  /**
   * Sets the name of the chat mode.
   *
   * @param chatModeName the chat mode name
   */
  public synchronized void setChatModeName(String chatModeName) {
    this.chatModeName = chatModeName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatModeName, modelName);
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
    UserPreference other = (UserPreference) obj;
    return Objects.equals(chatModeName, other.chatModeName) && Objects.equals(modelName, other.modelName);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("modelName", modelName);
    builder.add("chatModeName", chatModeName);
    return builder.toString();
  }
}
