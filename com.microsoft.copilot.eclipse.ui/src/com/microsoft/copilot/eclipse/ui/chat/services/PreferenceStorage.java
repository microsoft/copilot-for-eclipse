// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;

/**
 * Owns account-scoped chat preferences, asynchronous restoration and one serialized background writer.
 * Reads return detached snapshots; edits capture revisions without holding a UI-needed lock during file I/O.
 * Accepted saves retain their account path even after account changes or disposal.
 * Login transitions and successful LSP initialization retry unsuccessful loads once per lifecycle event.
 * They join pending loads, preserve ready choices, and never automatically retry failed saves.
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

  /**
   * Persistence status of the current account's ready choices, independent of loading.
   */
  public enum SaveState {
    SAVED, SAVING, FAILED
  }

  private final Object lock = new Object();
  private final CopilotLanguageServerConnection connection;
  private final AuthStatusManager auth;
  private final ExecutorService worker;
  private final ScheduledExecutorService timer;
  private final LongSupplier clock;
  private final FileAccess files;
  private final WritableValue<State> readiness;
  private final WritableValue<SaveState> saveStatus;
  private final CopilotAuthStatusListener authListener;
  private final IEventBroker eventBroker;
  private final EventHandler connectionListener;
  private long connectionIncarnation;
  private State state = State.UNAVAILABLE;
  private String account;
  private String notifiedAccount;
  private long generation;
  private Attempt attempt;
  private UserPreference preferences;
  private Path preferencePath;
  private long revision;
  private long savedRevision;
  private SaveState saveState = SaveState.SAVED;
  private final Deque<Save> pendingSaves = new ArrayDeque<>();
  private Save activeSave;
  private boolean writing;
  private volatile boolean shuttingDown;
  private boolean listenersDetached;
  private CompletableFuture<Boolean> shutdownSave;
  private long shutdownGeneration;
  private long shutdownRevision;
  private final Set<Path> shutdownFailedPaths = new HashSet<>();

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
        System::nanoTime, new PreferenceFileAccess());
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
    this.saveStatus = new WritableValue<>(realm, SaveState.SAVED, SaveState.class);
    this.account = currentAccount();
    this.notifiedAccount = account;
    this.authListener = status -> authenticationChanged();
    auth.addCopilotAuthStatusListener(authListener);
    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    this.connectionListener = event -> {
      if (event.getProperty(IEventBroker.DATA) instanceof Long incarnation) {
        synchronized (lock) {
          if (shuttingDown || state == State.DISPOSED || incarnation <= connectionIncarnation) {
            return;
          }
          connectionIncarnation = incarnation;
        }
        retry();
      }
    };
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_LANGUAGE_SERVER_INITIALIZED, null, connectionListener, false);
    } else {
      CopilotCore.LOGGER.error(new IllegalStateException("Cannot subscribe to language server initialization"));
    }
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
   * Returns a detached snapshot only when preferences are ready for the current account.
   *
   * @return loaded preferences, or {@code null} when unavailable
   */
  public UserPreference getReadyPreferences() {
    refreshAccount();
    synchronized (lock) {
      return state == State.READY && Objects.equals(account, currentAccount()) ? copy(preferences) : null;
    }
  }

  /**
   * Returns the UI-Realm save status. A failed save never changes preference readiness.
   *
   * @return the observable persistence status
   */
  public IObservableValue<SaveState> getSaveStatus() {
    return saveStatus;
  }

  /**
   * Returns whether current ready choices have not yet been persisted.
   *
   * @return whether the current account has unsaved changes
   */
  public boolean isDirty() {
    refreshAccount();
    synchronized (lock) {
      return state == State.READY && revision != savedRevision;
    }
  }

  /**
   * Applies a short in-memory edit immediately and queues its account-bound snapshot for saving.
   * The edit must not perform I/O. Neither the edit argument nor returned snapshots own shared state.
   *
   * @param edit the preference change
   * @return whether the ready preferences changed
   */
  public boolean update(Consumer<UserPreference> edit) {
    Objects.requireNonNull(edit);
    refreshAccount();
    synchronized (lock) {
      if (shuttingDown || state != State.READY || !Objects.equals(account, currentAccount())) {
        CopilotCore.LOGGER.error(new IllegalStateException("Cannot update chat preferences before they are ready"));
        return false;
      }
      UserPreference updated = copy(preferences);
      edit.accept(updated);
      if (updated.equals(preferences)) {
        return false;
      }
      preferences = copy(updated);
      revision++;
      enqueueSave();
    }
    dispatchWriter();
    return true;
  }

  /**
   * Retries the latest dirty snapshot using its ready path, without starting another path RPC.
   */
  public void persist() {
    refreshAccount();
    synchronized (lock) {
      if (shuttingDown || state != State.READY || revision == savedRevision
          || !Objects.equals(account, currentAccount())) {
        return;
      }
      enqueueSave();
    }
    dispatchWriter();
  }

  /**
   * Stops ordinary work and recovery, and submits the latest dirty snapshot to the existing writer.
   * Previously accepted account-bound writes participate even if the current account has no edits.
   * Never resolves a path or waits for loading or file I/O. Repeated calls share the same outcome.
   * The workbench owns the bounded wait; an accepted OS write may finish after that wait or disposal.
   *
   * @return a read-only completion indicating whether all pending saves succeeded (or nothing needed saving)
   */
  public CompletionStage<Boolean> beginShutdown() {
    Attempt previous;
    boolean nothingToSave;
    synchronized (lock) {
      if (shutdownSave != null) {
        return shutdownSave.minimalCompletionStage();
      }
      shuttingDown = true;
      shutdownSave = new CompletableFuture<>();
      previous = attempt;
      attempt = null;
      if (state == State.READY && preferencePath != null && revision != savedRevision) {
        enqueueSave();
      }
      Save last = pendingSaves.isEmpty() ? activeSave : pendingSaves.peekLast();
      nothingToSave = last == null;
      if (last != null) {
        shutdownGeneration = last.generation;
        shutdownRevision = last.revision;
      }
    }
    detachListeners();
    cancel(previous);
    if (nothingToSave) {
      shutdownSave.complete(true);
    } else {
      dispatchWriter();
    }
    return shutdownSave.minimalCompletionStage();
  }

  private void enqueueSave() {
    if (activeSave != null && activeSave.generation == generation && activeSave.revision == revision) {
      return;
    }
    pendingSaves.removeIf(save -> save.generation == generation);
    pendingSaves.addLast(new Save(generation, account, revision, preferencePath, copy(preferences)));
    saveState = SaveState.SAVING;
    publishSaveStatus(generation);
  }

  private void dispatchWriter() {
    synchronized (lock) {
      if (writing || pendingSaves.isEmpty()) {
        return;
      }
      writing = true;
    }
    try {
      worker.execute(this::writePending);
    } catch (RuntimeException exception) {
      CompletableFuture<Boolean> completion;
      synchronized (lock) {
        writing = false;
        pendingSaves.clear();
        if (state == State.READY && revision != savedRevision) {
          saveState = SaveState.FAILED;
          publishSaveStatus(generation);
        }
        completion = shutdownSave;
      }
      CopilotCore.LOGGER.error("Failed to schedule chat preference saving", exception);
      if (completion != null) {
        completion.complete(false);
      }
    }
  }

  private void writePending() {
    while (true) {
      Save save;
      CompletableFuture<Boolean> completion;
      synchronized (lock) {
        save = pendingSaves.pollFirst();
        activeSave = save;
        if (save == null) {
          writing = false;
          return;
        }
      }
      boolean successful = false;
      boolean shutdownSuccessful;
      try {
        files.write(save.path, GSON.toJson(save.preferences));
        successful = true;
      } catch (IOException | RuntimeException exception) {
        CopilotCore.LOGGER.error("Failed to save chat preferences", exception);
      }
      synchronized (lock) {
        activeSave = null;
        if (state == State.READY && generation == save.generation
            && Objects.equals(account, save.account) && Objects.equals(account, currentAccount())) {
          if (successful) {
            savedRevision = save.revision;
          }
          saveState = revision == savedRevision ? SaveState.SAVED
              : pendingSaves.stream().anyMatch(pending -> pending.generation == generation)
                  ? SaveState.SAVING : SaveState.FAILED;
          publishSaveStatus(generation);
        }
        completion = save.generation == shutdownGeneration && save.revision == shutdownRevision ? shutdownSave : null;
        if (shuttingDown) {
          if (successful) {
            shutdownFailedPaths.remove(save.path);
          } else {
            shutdownFailedPaths.add(save.path);
          }
        }
        shutdownSuccessful = shutdownFailedPaths.isEmpty();
      }
      if (completion != null) {
        completion.complete(shutdownSuccessful);
      }
    }
  }

  private void publishSaveStatus(long version) {
    if (shuttingDown) {
      return;
    }
    saveStatus.getRealm().asyncExec(() -> {
      refreshAccount();
      synchronized (lock) {
        if (!shuttingDown && state != State.DISPOSED && generation == version && !saveStatus.isDisposed()) {
          saveStatus.setValue(saveState);
        }
      }
    });
  }

  private static UserPreference copy(UserPreference source) {
    UserPreference snapshot = new UserPreference();
    snapshot.setChatModel(source.getChatModel());
    snapshot.setChatModeName(source.getChatModeName());
    snapshot.setSkipGitHubJobConfirmDialog(source.isSkipGitHubJobConfirmDialog());
    snapshot.setUserInputs(source.getUserInputs() == null ? null : new ArrayList<>(source.getUserInputs()));
    snapshot.setReasoningEfforts(source.getReasoningEffortSnapshot());
    snapshot.setContextWindows(source.getContextWindowSnapshot());
    return snapshot;
  }

  /**
   * Invalidates pending work and detaches listeners without waiting for background operations.
   */
  public void dispose() {
    Attempt previous;
    CompletableFuture<Boolean> completion;
    synchronized (lock) {
      if (state == State.DISPOSED) {
        return;
      }
      previous = clear(State.DISPOSED);
      completion = shutdownSave;
    }
    detachListeners();
    cancel(previous);
    // Accepted snapshots may finish on the shared writer; disposal never waits for OS I/O.
    dispatchWriter();
    worker.shutdown();
    timer.shutdownNow();
    if (completion != null) {
      completion.complete(false);
    }
    readiness.getRealm().asyncExec(() -> {
      if (!readiness.isDisposed()) {
        readiness.setValue(State.DISPOSED);
        readiness.dispose();
      }
      if (!saveStatus.isDisposed()) {
        saveStatus.dispose();
      }
    });
  }

  private void detachListeners() {
    synchronized (lock) {
      if (listenersDetached) {
        return;
      }
      listenersDetached = true;
    }
    auth.removeCopilotAuthStatusListener(authListener);
    if (eventBroker != null) {
      eventBroker.unsubscribe(connectionListener);
    }
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
      if (shuttingDown || state == State.DISPOSED || Objects.equals(account, current)) {
        return;
      }
      account = current;
      previous = clear(State.UNAVAILABLE);
      version = generation;
    }
    cancel(previous);
    publishState(version, State.UNAVAILABLE);
    publishSaveStatus(version);
  }

  private void authenticationChanged() {
    boolean changed;
    synchronized (lock) {
      String current = currentAccount();
      changed = !Objects.equals(notifiedAccount, current);
      notifiedAccount = current;
    }
    refreshAccount();
    if (changed) {
      retry();
    }
  }

  private Attempt clear(State next) {
    final Attempt previous = attempt;
    attempt = null;
    preferences = null;
    preferencePath = null;
    revision = 0;
    savedRevision = 0;
    saveState = SaveState.SAVED;
    state = next;
    generation++;
    return previous;
  }

  private void start(boolean retry) {
    refreshAccount();
    Attempt next;
    synchronized (lock) {
      if (shuttingDown || account == null || state == State.DISPOSED || state == State.LOADING || state == State.READY
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
    return !shuttingDown && state == State.LOADING && attempt == pending && generation == pending.generation
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
        if (shuttingDown || generation != version || state != next || readiness.isDisposed()) {
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

  private record Save(long generation, String account, long revision, Path path, UserPreference preferences) {
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
