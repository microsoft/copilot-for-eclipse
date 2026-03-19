package com.microsoft.copilot.eclipse.core.chat;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Preferences per GitHub user. All the getters and setters are synchronized due to that the ChatBaseService holds a
 * shared (single) reference to the user preference. synchronized modifies makes sure the update to the instance are
 * thread safe.
 */
public class UserPreference {

  private String chatModel;
  private String chatModeName;
  private List<String> userInputs;
  private boolean skipGitHubJobConfirmDialog;

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

  public synchronized List<String> getUserInputs() {
    return userInputs;
  }

  public synchronized void setUserInputs(List<String> userInputs) {
    this.userInputs = userInputs;
  }

  public synchronized boolean isSkipGitHubJobConfirmDialog() {
    return skipGitHubJobConfirmDialog;
  }

  public synchronized void setSkipGitHubJobConfirmDialog(boolean skipGitHubJobConfirmDialog) {
    this.skipGitHubJobConfirmDialog = skipGitHubJobConfirmDialog;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatModeName, chatModel, userInputs, skipGitHubJobConfirmDialog);
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
    return Objects.equals(chatModeName, other.chatModeName) && Objects.equals(chatModel, other.chatModel)
        && Objects.equals(userInputs, other.userInputs)
        && skipGitHubJobConfirmDialog == other.skipGitHubJobConfirmDialog;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("chatModel", chatModel);
    builder.append("chatModeName", chatModeName);
    builder.append("userInputs", userInputs);
    builder.append("skipGitHubJobConfirmDialog", skipGitHubJobConfirmDialog);
    return builder.toString();
  }
}
