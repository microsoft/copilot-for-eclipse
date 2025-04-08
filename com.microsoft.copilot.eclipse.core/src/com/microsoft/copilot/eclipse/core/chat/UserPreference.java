package com.microsoft.copilot.eclipse.core.chat;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Preferences per GitHub user. All the getters and setters are synchronized due to that the ChatBaseService holds a
 * shared (single) reference to the user preference. synchronized modifies makes sure the update to the instance are
 * thread safe.
 */
public class UserPreference {

  private String chatModel;
  private String chatModeName;

  /**
   * Gets the id of the Chat model.
   *
   * @return the model id
   */
  public synchronized String getChatModel() {
    return chatModel;
  }

  /**
   * Sets the id of the Chat model.
   *
   * @param id the model id
   */
  public synchronized void setChatModel(String id) {
    this.chatModel = id;
  }

  /**
   * Gets the name of the chat mode. For all the available modes, see:
   * {@link com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode}.
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
    return Objects.hash(chatModeName, chatModel);
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
    return Objects.equals(chatModeName, other.chatModeName) && Objects.equals(chatModel, other.chatModel);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("chatModel", chatModel);
    builder.add("chatModeName", chatModeName);
    return builder.toString();
  }
}
