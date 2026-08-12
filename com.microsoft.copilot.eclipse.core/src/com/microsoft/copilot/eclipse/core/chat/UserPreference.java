// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
   * User-selected reasoning effort keyed by the composite model key (matching the {@link #chatModel} format).
   */
  private volatile Map<String, String> reasoningEffortByModel = Map.of();

  /**
   * User-selected context-window size (a price tier's {@code maxContext} token count) keyed by the composite model
   * key (matching the {@link #chatModel} format).
   */
  private volatile Map<String, Integer> contextWindowByModel = Map.of();

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

  /**
   * Returns the user-selected reasoning effort for the given model key, or {@code null} when the user has not made an
   * explicit selection.
   *
   * @param modelKey the composite model key (matching the {@link #getChatModel()} format)
   * @return the previously selected reasoning effort, or {@code null}
   */
  public String getReasoningEffort(String modelKey) {
    if (modelKey == null) {
      return null;
    }
    return reasoningEffortByModel.get(modelKey);
  }

  /**
   * Returns an immutable snapshot of the model-key to reasoning-effort map. Useful as an observable value that
   * changes by equality whenever any individual entry is added, updated, or removed.
   *
   * @return an immutable snapshot of the current reasoning-effort map (never {@code null})
   */
  public Map<String, String> getReasoningEffortSnapshot() {
    return reasoningEffortByModel;
  }

  /**
   * Stores the user-selected reasoning effort for the given model key. Passing a {@code null} effort clears any
   * previously stored value for that key.
   *
   * <p>The underlying map is replaced atomically with a fresh immutable snapshot via {@link Map#copyOf}, so concurrent
   * readers (including Gson reflectively serializing this preference on a background thread) always observe either
   * the old or the new snapshot, never a partially mutated map.
   *
   * @param modelKey the composite model key (matching the {@link #getChatModel()} format)
   * @param reasoningEffort the reasoning effort to store, or {@code null} to clear
   */
  public synchronized void setReasoningEffort(String modelKey, String reasoningEffort) {
    if (modelKey == null) {
      return;
    }
    Map<String, String> current = reasoningEffortByModel;
    if (reasoningEffort == null) {
      if (!current.containsKey(modelKey)) {
        return;
      }
      Map<String, String> next = new HashMap<>(current);
      next.remove(modelKey);
      reasoningEffortByModel = Map.copyOf(next);
      return;
    }
    if (reasoningEffort.equals(current.get(modelKey))) {
      return;
    }
    Map<String, String> next = new HashMap<>(current);
    next.put(modelKey, reasoningEffort);
    reasoningEffortByModel = Map.copyOf(next);
  }

  /**
   * Atomically replaces the entire reasoning-effort map with the given snapshot, dropping any keys not present in
   * {@code reasoningEffortsByModel}. {@code null} entries in the input map are ignored. Returns {@code true} when
   * the new snapshot differs from the previous one (so callers can decide whether to persist or notify observers).
   *
   * @param reasoningEffortsByModel the new reasoning-effort map keyed by composite model key (matching the
   *     {@link #getChatModel()} format); may be {@code null} or empty to clear all entries
   * @return {@code true} when the stored map changed, {@code false} otherwise
   */
  public synchronized boolean setReasoningEfforts(Map<String, String> reasoningEffortsByModel) {
    Map<String, String> updatedReasoningEfforts;
    if (reasoningEffortsByModel == null || reasoningEffortsByModel.isEmpty()) {
      updatedReasoningEfforts = Map.of();
    } else {
      Map<String, String> copy = new HashMap<>();
      for (Map.Entry<String, String> entry : reasoningEffortsByModel.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          copy.put(entry.getKey(), entry.getValue());
        }
      }
      updatedReasoningEfforts = Map.copyOf(copy);
    }
    if (updatedReasoningEfforts.equals(this.reasoningEffortByModel)) {
      return false;
    }
    this.reasoningEffortByModel = updatedReasoningEfforts;
    return true;
  }

  /**
   * Returns the user-selected context-window size for the given model key, or {@code null} when the user has not made
   * an explicit selection.
   *
   * @param modelKey the composite model key (matching the {@link #getChatModel()} format)
   * @return the previously selected context-window size, or {@code null}
   */
  public Integer getContextWindow(String modelKey) {
    if (modelKey == null) {
      return null;
    }
    return contextWindowByModel.get(modelKey);
  }

  /**
   * Returns an immutable snapshot of the model-key to context-window map. Useful as an observable value that changes
   * by equality whenever any individual entry is added, updated, or removed.
   *
   * @return an immutable snapshot of the current context-window map (never {@code null})
   */
  public Map<String, Integer> getContextWindowSnapshot() {
    return contextWindowByModel;
  }

  /**
   * Stores the user-selected context-window size for the given model key. Passing a {@code null} value clears any
   * previously stored value for that key.
   *
   * <p>The underlying map is replaced atomically with a fresh immutable snapshot via {@link Map#copyOf}, so concurrent
   * readers (including Gson reflectively serializing this preference on a background thread) always observe either
   * the old or the new snapshot, never a partially mutated map.
   *
   * @param modelKey the composite model key (matching the {@link #getChatModel()} format)
   * @param contextWindow the context-window size to store, or {@code null} to clear
   * @return {@code true} when the stored context window changed, {@code false} otherwise
   */
  public synchronized boolean setContextWindow(String modelKey, Integer contextWindow) {
    if (modelKey == null) {
      return false;
    }
    Map<String, Integer> current = contextWindowByModel;
    if (contextWindow == null) {
      if (!current.containsKey(modelKey)) {
        return false;
      }
      Map<String, Integer> next = new HashMap<>(current);
      next.remove(modelKey);
      contextWindowByModel = Map.copyOf(next);
      return true;
    }
    if (contextWindow.equals(current.get(modelKey))) {
      return false;
    }
    Map<String, Integer> next = new HashMap<>(current);
    next.put(modelKey, contextWindow);
    contextWindowByModel = Map.copyOf(next);
    return true;
  }

  /**
   * Atomically replaces the entire context-window map with the given snapshot, dropping any keys not present in
   * {@code contextWindowsByModel}. {@code null} entries in the input map are ignored. Returns {@code true} when the
   * new snapshot differs from the previous one (so callers can decide whether to persist or notify observers).
   *
   * @param contextWindowsByModel the new context-window map keyed by composite model key (matching the
   *     {@link #getChatModel()} format); may be {@code null} or empty to clear all entries
   * @return {@code true} when the stored map changed, {@code false} otherwise
   */
  public synchronized boolean setContextWindows(Map<String, Integer> contextWindowsByModel) {
    Map<String, Integer> updatedContextWindows;
    if (contextWindowsByModel == null || contextWindowsByModel.isEmpty()) {
      updatedContextWindows = Map.of();
    } else {
      Map<String, Integer> copy = new HashMap<>();
      for (Map.Entry<String, Integer> entry : contextWindowsByModel.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          copy.put(entry.getKey(), entry.getValue());
        }
      }
      updatedContextWindows = Map.copyOf(copy);
    }
    if (updatedContextWindows.equals(this.contextWindowByModel)) {
      return false;
    }
    this.contextWindowByModel = updatedContextWindows;
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chatModeName, chatModel, userInputs, skipGitHubJobConfirmDialog, reasoningEffortByModel,
        contextWindowByModel);
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
        && skipGitHubJobConfirmDialog == other.skipGitHubJobConfirmDialog
        && Objects.equals(reasoningEffortByModel, other.reasoningEffortByModel)
        && Objects.equals(contextWindowByModel, other.contextWindowByModel);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("chatModel", chatModel);
    builder.append("chatModeName", chatModeName);
    builder.append("userInputs", userInputs);
    builder.append("skipGitHubJobConfirmDialog", skipGitHubJobConfirmDialog);
    builder.append("reasoningEffortByModel", reasoningEffortByModel);
    builder.append("contextWindowByModel", contextWindowByModel);
    return builder.toString();
  }
}
