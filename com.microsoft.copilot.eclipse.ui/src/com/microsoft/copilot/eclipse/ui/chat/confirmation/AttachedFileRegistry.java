// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Tracks files explicitly attached by the user in the context panel.
 *
 * <p>New files are first stored in a {@code pending} set (not yet bound
 * to any conversation ID). The confirmation handler checks pending files
 * first, so auto-approve works even before the real conversation ID
 * arrives from CLS. Once the real ID is known, call
 * {@link #flushPending(String)} to move them into per-conversation
 * storage.
 */
public class AttachedFileRegistry {

  private final Map<String, Set<String>> attachedPaths =
      Collections.synchronizedMap(new LinkedHashMap<>());

  /** Files waiting for a stable conversation ID. */
  private final Set<String> pendingFiles =
      Collections.synchronizedSet(new HashSet<>());

  /**
   * Stages file paths for auto-approve before the conversation ID is
   * known. These are checked by {@link #isAttachedFile} immediately.
   */
  public void addPending(Collection<String> filePaths) {
    if (filePaths == null || filePaths.isEmpty()) {
      return;
    }
    filePaths.stream()
        .filter(StringUtils::isNotBlank)
        .map(AttachedFileRegistry::toComparisonKey)
        .forEach(pendingFiles::add);
  }

  /**
   * Moves pending files into per-conversation storage under the given
   * conversation ID, then clears the pending set.
   */
  public void flushPending(String conversationId) {
    if (StringUtils.isBlank(conversationId) || pendingFiles.isEmpty()) {
      return;
    }
    Set<String> flushed;
    synchronized (pendingFiles) {
      flushed = new HashSet<>(pendingFiles);
      pendingFiles.clear();
    }
    evictOldestIfNeeded();
    synchronized (attachedPaths) {
      attachedPaths.merge(conversationId, flushed, (a, b) -> {
        Set<String> merged = new HashSet<>(a);
        merged.addAll(b);
        return merged;
      });
    }
  }

  /**
   * Records files for an existing conversation (continued turns).
   */
  public void addAttachedFiles(String conversationId,
      Collection<String> filePaths) {
    if (filePaths == null || filePaths.isEmpty()
        || StringUtils.isBlank(conversationId)) {
      return;
    }
    Set<String> keys = filePaths.stream()
        .filter(StringUtils::isNotBlank)
        .map(AttachedFileRegistry::toComparisonKey)
        .collect(Collectors.toSet());
    if (keys.isEmpty()) {
      return;
    }
    evictOldestIfNeeded();
    synchronized (attachedPaths) {
      attachedPaths.merge(conversationId, keys, (a, b) -> {
        Set<String> merged = new HashSet<>(a);
        merged.addAll(b);
        return merged;
      });
    }
  }

  /**
   * Returns {@code true} when the given file was explicitly attached
   * by the user — either in the pending set or for the given conversation.
   */
  public boolean isAttachedFile(String conversationId, String filePath) {
    if (StringUtils.isBlank(filePath)) {
      return false;
    }
    String key = toComparisonKey(filePath);
    // Check pending files first (before conversation ID is known)
    if (pendingFiles.contains(key)) {
      return true;
    }
    Set<String> paths = attachedPaths.get(conversationId);
    return paths != null && paths.contains(key);
  }

  /** Removes all tracked data for a conversation. */
  public void clearConversation(String conversationId) {
    attachedPaths.remove(conversationId);
  }

  /** Discards any pending (pre-conversation) files. */
  public void clearPending() {
    pendingFiles.clear();
  }

  private void evictOldestIfNeeded() {
    synchronized (attachedPaths) {
      while (attachedPaths.size() >= ConfirmationHandler.MAX_SESSION_CONVERSATIONS) {
        var it = attachedPaths.entrySet().iterator();
        if (it.hasNext()) {
          it.next();
          it.remove();
        }
      }
    }
  }

  private static String toComparisonKey(String path) {
    return ConfirmationHandler.normalizePath(path);
  }
}
