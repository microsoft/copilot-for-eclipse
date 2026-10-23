// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Base class for chat services.
 */
public abstract class ChatBaseService {
  protected static final Gson gson = new Gson();
  protected static final String PREF_FILE_NAME = "pref.json";

  protected CopilotLanguageServerConnection lsConnection;
  protected AuthStatusManager authStatusManager;

  protected volatile String persistentPath;
  private static UserPreference userPreference;
  // Not a monitor, so that shutdown can give up waiting while a preference load waits for the CLS
  private final ReentrantLock preferenceLock = new ReentrantLock();

  /**
   * Constructor for the ChatBaseService.
   */
  protected ChatBaseService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    this.lsConnection = lsConnection;
    this.authStatusManager = authStatusManager;
  }

  /**
   * Get User Preference.
   */
  protected UserPreference getUserPreference() {
    preferenceLock.lock();
    try {
      return loadUserPreference();
    } finally {
      preferenceLock.unlock();
    }
  }

  private UserPreference loadUserPreference() {
    if (userPreference != null) {
      return userPreference;
    }

    Path path = getPersistentFilePath();
    if (path != null) {
      try {
        String jsonContent = PlatformUtils.readFileContent(path);
        if (!jsonContent.isEmpty()) {
          userPreference = gson.fromJson(jsonContent, UserPreference.class);
          if (userPreference != null) {
            return userPreference;
          }
        }
      } catch (JsonSyntaxException e) {
        CopilotCore.LOGGER.error("Failed to get user preference, will generate a new one.", e);
      }
    }

    userPreference = new UserPreference();
    return userPreference;
  }

  /**
   * Clear User Preference.
   */
  protected void clearUserPreferenceCache() {
    userPreference = null;
  }

  /**
   * Persist User Preference.
   */
  public void persistUserPreference() {
    persistUserPreference(null);
  }

  /**
   * Persist User Preference, waiting at most the given time for the language server to provide the storage path.
   *
   * @param timeout the maximum time to wait, or {@code null} to wait without limit
   */
  protected void persistUserPreference(@Nullable Duration timeout) {
    long deadline = timeout == null ? 0 : System.nanoTime() + timeout.toNanos();
    // Resolve the path outside the lock, a pending preference load may hold it while waiting for the CLS
    Path path = getPersistentFilePath(timeout);
    if (path == null) {
      return;
    }

    try {
      if (timeout == null) {
        preferenceLock.lock();
      } else if (!preferenceLock.tryLock(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)) {
        CopilotCore.LOGGER.info("User preference is still loading, skipping save");
        return;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    try {
      String jsonContent = gson.toJson(userPreference);
      PlatformUtils.writeFileContent(path, jsonContent);
    } finally {
      preferenceLock.unlock();
    }
  }

  /**
   * Ensures operations run in the correct Realm.
   *
   * @param runnable The code to execute in the UI Realm.
   */
  protected void ensureRealm(Runnable runnable) {
    // If we're already in the UI thread
    if (Display.getCurrent() != null) {
      Realm realm = Realm.getDefault();
      if (realm == null) {
        realm = DisplayRealm.getRealm(Display.getCurrent());
      }
      Realm.runWithDefault(realm, runnable::run);
    } else {
      SwtUtils.invokeOnDisplayThread(() -> {
        Realm realm = DisplayRealm.getRealm(Display.getDefault());
        Realm.runWithDefault(realm, runnable::run);
      });
    }
  }

  /**
   * Get the path for the persistent file.
   */
  private @Nullable Path getPersistentFilePath() {
    return getPersistentFilePath(null);
  }

  /**
   * Get the path for the persistent file, waiting at most the given time for the language server to provide it.
   *
   * @param timeout the maximum time to wait, or {@code null} to wait without limit
   */
  private @Nullable Path getPersistentFilePath(@Nullable Duration timeout) {
    if (!this.authStatusManager.isSignedIn()) {
      return null;
    }
    if (this.persistentPath == null) {
      try {
        CompletableFuture<ChatPersistence> request = this.lsConnection.persistence();
        ChatPersistence chatPersistence = timeout == null ? request.get()
            : request.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        this.persistentPath = chatPersistence.getPath();
      } catch (InterruptedException | ExecutionException e) {
        CopilotCore.LOGGER.error("Failed to get persistent path", e);
        return null;
      } catch (TimeoutException e) {
        CopilotCore.LOGGER.info("Language server did not provide the persistent path in time, skipping");
        return null;
      }
    }

    final String user = this.authStatusManager.getUserName();
    if (StringUtils.isBlank(user)) {
      CopilotCore.LOGGER.error(new IllegalStateException("User name is empty"));
      return null;
    }

    return Paths.get(this.persistentPath, user, PREF_FILE_NAME);
  }

  /**
   * Update the value of an observable in its realm.
   *
   * @param observable The observable to update.
   * @param value The new value to set.
   */
  protected <T> void updateObservable(IObservableValue<T> observable, final T value) {
    if (observable != null) {
      observable.getRealm().asyncExec(() -> observable.setValue(value));
    }
  }
}
