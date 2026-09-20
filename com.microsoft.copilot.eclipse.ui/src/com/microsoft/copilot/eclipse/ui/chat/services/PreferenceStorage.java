// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;

/**
 * Owns account-scoped chat preferences and their asynchronous initial restoration.
 */
public class PreferenceStorage {
  private static final Gson GSON = new Gson();
  private static final long LOAD_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(15);

  /**
   * Availability of preferences for the current account.
   */
  public enum State {
    UNAVAILABLE, LOADING, READY, FAILED, DISPOSED
  }

  private final Object lock = new Object();
  private final Object saveLock = new Object();
  private final CopilotLanguageServerConnection connection;
  private final AuthStatusManager auth;
  private final ExecutorService worker;
  private final ScheduledExecutorService timer;
  private final LongSupplier clock;
  private final FileAccess files;
  private final WritableValue<State> readiness;
  private final CopilotAuthStatusListener authListener;
  private State state = State.UNAVAILABLE;
  private String account;
  private long generation;
  private Attempt attempt;
  private UserPreference preferences;
  private Path preferencePath;

  /**
   * Creates storage without starting RPC or file work.
   *
   * @param connection the existing language server connection
   * @param auth the existing account manager
   */
  public PreferenceStorage(CopilotLanguageServerConnection connection, AuthStatusManager auth) {
    this(connection, auth, DisplayRealm.getRealm(Display.getDefault()),
        Executors.newCachedThreadPool(runnable -> daemonThread(runnable, "Copilot preference loading")),
        Executors.newSingleThreadScheduledExecutor(runnable -> daemonThread(runnable, "Copilot preference deadline")),
        System::nanoTime, new FileAccess() {
          @Override
          public String read(Path path) throws IOException {
            return Files.readString(path);
          }

          @Override
          public void write(Path path, String content) throws IOException {
            if (Files.notExists(path)) {
              Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
          }
        });
  }

  PreferenceStorage(CopilotLanguageServerConnection connection, AuthStatusManager auth, Realm realm,
      ExecutorService worker, ScheduledExecutorService timer, LongSupplier clock, FileAccess files) {
    this.connection = connection;
    this.auth = auth;
    this.worker = worker;
    this.timer = timer;
    this.clock = clock;
    this.files = files;
    this.readiness = new WritableValue<>(realm, State.UNAVAILABLE, State.class);
    this.account = currentAccount();
    this.authListener = status -> refreshAccount();
    auth.addCopilotAuthStatusListener(authListener);
  }

  /**
   * Returns current readiness without waiting for RPC, file work, or the UI thread.
   *
   * @return readiness applicable to the current account
   */
  public State getState() {
    refreshAccount();
    synchronized (lock) {
      return state == State.DISPOSED || Objects.equals(account, currentAccount()) ? state : State.UNAVAILABLE;
    }
  }

  /**
   * Returns the UI-Realm observable, available before initialization starts.
   *
   * @return the readiness observable
   */
  public IObservableValue<State> getReadiness() {
    return readiness;
  }

  /**
   * Starts the first load, coalescing callers without implicitly retrying failures.
   */
  public void initialize() {
    start(false);
  }

  /**
   * Explicitly retries unavailable or failed preferences without replacing ready choices.
   */
  public void retry() {
    start(true);
  }

  /**
   * Returns the authoritative preference object only when ready for the current account.
   *
   * @return loaded preferences, or {@code null} when unavailable
   */
  public UserPreference getReadyPreferences() {
    refreshAccount();
    synchronized (lock) {
      return state == State.READY && Objects.equals(account, currentAccount()) ? preferences : null;
    }
  }

  /**
   * Synchronously saves ready preferences to their already resolved account path, without RPC.
   */
  public void persist() {
    persist(getReadyPreferences());
  }

  /**
   * Saves a captured preference only while it remains authoritative for the current account.
   */
  void persist(UserPreference expected) {
    if (expected == null || getReadyPreferences() != expected) {
      return;
    }
    synchronized (saveLock) {
      refreshAccount();
      try {
        Path path;
        String json;
        synchronized (lock) {
          if (state != State.READY || preferences != expected
              || !Objects.equals(account, currentAccount())) {
            return;
          }
          path = preferencePath;
          synchronized (expected) {
            json = GSON.toJson(expected);
          }
        }
        files.write(path, json);
      } catch (IOException | RuntimeException exception) {
        CopilotCore.LOGGER.error("Failed to save chat preferences", exception);
      }
    }
  }

  /**
   * Invalidates pending work and detaches listeners without waiting for background operations.
   */
  public void dispose() {
    Attempt previous;
    synchronized (lock) {
      if (state == State.DISPOSED) {
        return;
      }
      previous = clear(State.DISPOSED);
    }
    auth.removeCopilotAuthStatusListener(authListener);
    cancel(previous);
    worker.shutdownNow();
    timer.shutdownNow();
    readiness.getRealm().asyncExec(() -> {
      if (!readiness.isDisposed()) {
        readiness.setValue(State.DISPOSED);
        readiness.dispose();
      }
    });
  }

  private static Thread daemonThread(Runnable runnable, String name) {
    Thread thread = new Thread(runnable, name);
    thread.setDaemon(true);
    return thread;
  }

  private String currentAccount() {
    String user = auth.getUserName();
    return auth.isSignedIn() && StringUtils.isNotBlank(user) ? user : null;
  }

  private void refreshAccount() {
    Attempt previous;
    long version;
    synchronized (lock) {
      String current = currentAccount();
      if (state == State.DISPOSED || Objects.equals(account, current)) {
        return;
      }
      account = current;
      previous = clear(State.UNAVAILABLE);
      version = generation;
    }
    cancel(previous);
    publishState(version, State.UNAVAILABLE);
  }

  private Attempt clear(State next) {
    final Attempt previous = attempt;
    attempt = null;
    preferences = null;
    preferencePath = null;
    state = next;
    generation++;
    return previous;
  }

  private void start(boolean retry) {
    refreshAccount();
    Attempt next;
    synchronized (lock) {
      if (account == null || state == State.DISPOSED || state == State.LOADING || state == State.READY
          || (!retry && state == State.FAILED)) {
        return;
      }
      state = State.LOADING;
      next = new Attempt(++generation, account, clock.getAsLong());
      attempt = next;
    }
    publishState(next.generation, State.LOADING);
    try {
      ScheduledFuture<?> timeout = timer.schedule(() -> fail(next, null), 15, TimeUnit.SECONDS);
      synchronized (lock) {
        if (attempt == next) {
          next.timeout = timeout;
        } else {
          timeout.cancel(false);
        }
      }
      dispatch(next, () -> resolve(next));
    } catch (RuntimeException exception) {
      fail(next, exception);
    }
  }

  private void dispatch(Attempt pending, Runnable runnable) {
    FutureTask<Void> task = new FutureTask<>(() -> {
      if (isCurrent(pending)) {
        runnable.run();
      }
    }, null);
    synchronized (lock) {
      if (!matches(pending)) {
        return;
      }
      pending.work = task;
    }
    try {
      worker.execute(task);
    } catch (RuntimeException exception) {
      fail(pending, exception);
    }
  }

  private void resolve(Attempt pending) {
    try {
      CompletableFuture<ChatPersistence> rpc = connection.persistence();
      if (rpc == null) {
        throw new IllegalStateException("Persistence request returned no future");
      }
      boolean applicable;
      synchronized (lock) {
        applicable = matches(pending);
        if (applicable) {
          pending.rpc = rpc;
        }
      }
      if (!applicable) {
        rpc.cancel(true);
        return;
      }
      rpc.whenComplete((result, failure) -> {
        if (failure != null) {
          fail(pending, failure);
        } else {
          dispatch(pending, () -> read(pending, result));
        }
      });
    } catch (RuntimeException exception) {
      fail(pending, exception);
    }
  }

  private void read(Attempt pending, ChatPersistence result) {
    try {
      if (result == null || StringUtils.isBlank(result.getPath())) {
        throw new IllegalArgumentException("Persistence response has no path");
      }
      Path directory = Path.of(result.getPath());
      if (!directory.isAbsolute()) {
        throw new IllegalArgumentException("Persistence path is not absolute");
      }
      Path path = directory.resolve(pending.account).resolve("pref.json");
      UserPreference restored;
      try {
        restored = parse(files.read(path));
      } catch (NoSuchFileException exception) {
        restored = new UserPreference();
      }
      publishPreferences(pending, path, restored);
    } catch (IOException | RuntimeException exception) {
      fail(pending, exception);
    }
  }

  private UserPreference parse(String json) throws IOException {
    rejectLegacyJsonExtensions(json);
    try (JsonReader reader = new JsonReader(new StringReader(json))) {
      reader.setLenient(false);
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
        throw new JsonSyntaxException("Preferences must contain an object");
      }
      // The adapter preserves JsonReader strictness, unlike Gson.fromJson(JsonReader, Class).
      UserPreference restored = GSON.getAdapter(UserPreference.class).read(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonSyntaxException("Unexpected content after preferences");
      }
      restored.setReasoningEfforts(restored.getReasoningEffortSnapshot());
      restored.setContextWindows(restored.getContextWindowSnapshot());
      return restored;
    }
  }

  private void rejectLegacyJsonExtensions(String json) {
    // Gson 2.10's non-lenient mode still accepts control characters, non-JSON escapes and mixed-case literals.
    // Reject those extensions before the reader validates the document's remaining syntax and types.
    boolean quoted = false;
    for (int index = 0; index < json.length(); index++) {
      char character = json.charAt(index);
      if (quoted) {
        if (character < 0x20) {
          throw new JsonSyntaxException("Unescaped control character in preferences");
        }
        if (character == '\\') {
          index++;
          if (index == json.length() || "\"\\/bfnrtu".indexOf(json.charAt(index)) < 0) {
            throw new JsonSyntaxException("Invalid escape in preferences");
          }
        } else if (character == '"') {
          quoted = false;
        }
      } else if (character == '"') {
        quoted = true;
      } else if ("tTfFnN".indexOf(character) >= 0) {
        int start = index;
        while (index + 1 < json.length() && Character.isLetter(json.charAt(index + 1))) {
          index++;
        }
        String literal = json.substring(start, index + 1);
        if (!"true".equals(literal) && !"false".equals(literal) && !"null".equals(literal)) {
          throw new JsonSyntaxException("Invalid literal in preferences");
        }
      }
    }
  }

  private void publishPreferences(Attempt pending, Path path, UserPreference restored) {
    readiness.getRealm().asyncExec(() -> {
      if (!isCurrent(pending)) {
        return;
      }
      synchronized (lock) {
        if (!matches(pending)) {
          return;
        }
        preferences = restored;
        preferencePath = path;
        state = State.READY;
        attempt = null;
        if (!readiness.isDisposed()) {
          readiness.setValue(State.READY);
        }
      }
      if (pending.timeout != null) {
        pending.timeout.cancel(false);
      }
    });
  }

  private boolean matches(Attempt pending) {
    return state == State.LOADING && attempt == pending && generation == pending.generation
        && Objects.equals(account, pending.account) && Objects.equals(account, currentAccount());
  }

  private boolean isCurrent(Attempt pending) {
    refreshAccount();
    synchronized (lock) {
      if (!matches(pending)) {
        return false;
      }
      if (clock.getAsLong() - pending.started < LOAD_TIMEOUT_NANOS) {
        return true;
      }
    }
    fail(pending, null);
    return false;
  }

  private void fail(Attempt pending, Throwable failure) {
    refreshAccount();
    long version;
    synchronized (lock) {
      if (!matches(pending)) {
        return;
      }
      clear(State.FAILED);
      version = generation;
    }
    cancel(pending);
    if (failure != null) {
      CopilotCore.LOGGER.error("Failed to load chat preferences", failure);
    }
    publishState(version, State.FAILED);
  }

  private void publishState(long version, State next) {
    readiness.getRealm().asyncExec(() -> {
      refreshAccount();
      synchronized (lock) {
        if (generation != version || state != next || readiness.isDisposed()) {
          return;
        }
        readiness.setValue(next);
      }
    });
  }

  private void cancel(Attempt pending) {
    if (pending == null) {
      return;
    }
    if (pending.timeout != null) {
      pending.timeout.cancel(false);
    }
    if (pending.work != null) {
      pending.work.cancel(false);
    }
    if (pending.rpc != null) {
      pending.rpc.cancel(true);
    }
  }

  interface FileAccess {
    String read(Path path) throws IOException;

    void write(Path path, String content) throws IOException;
  }

  /**
   * Account and lifecycle identity captured before dispatching any load work.
   */
  private static class Attempt {
    private final long generation;
    private final String account;
    private final long started;
    private volatile CompletableFuture<ChatPersistence> rpc;
    private volatile FutureTask<Void> work;
    private volatile ScheduledFuture<?> timeout;

    Attempt(long generation, String account, long started) {
      this.generation = generation;
      this.account = account;
      this.started = started;
    }
  }
}
