// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.databinding.observable.Realm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.State;

class PreferenceStorageTest {
  private final CopilotLanguageServerConnection connection = mock(CopilotLanguageServerConnection.class);
  private final AuthStatusManager auth = mock(AuthStatusManager.class);
  private final ExecutorService worker = mock(ExecutorService.class);
  private final ScheduledExecutorService timer = mock(ScheduledExecutorService.class);
  private final TestFileAccess files = mock(TestFileAccess.class);
  private final Queue<Runnable> work = new ConcurrentLinkedQueue<>();
  private final Queue<Runnable> deadlines = new ConcurrentLinkedQueue<>();
  private final TestRealm realm = new TestRealm();
  private final AtomicLong clock = new AtomicLong();
  private CompletableFuture<ChatPersistence> response;
  private PreferenceStorage storage;

  @BeforeEach
  void setUp() {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("alice");
    response = new CompletableFuture<>();
    when(connection.persistence()).thenAnswer(invocation -> response);
    Mockito.doAnswer(invocation -> {
      work.add(invocation.getArgument(0));
      return null;
    }).when(worker).execute(any(Runnable.class));
    when(timer.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
      deadlines.add(invocation.getArgument(0));
      return mock(ScheduledFuture.class);
    });
    storage = new PreferenceStorage(connection, auth, realm, worker, timer, clock::get, files);
  }

  @AfterEach
  void tearDown() {
    storage.dispose();
    realm.drain();
  }

  @Test
  void testUpdate_RapidChanges_AreImmediateAndCoalesceBeforeWriting() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();

    storage.update(preference -> preference.setChatModel("first"));
    storage.update(preference -> preference.setChatModel("latest"));

    assertEquals("latest", storage.getReadyPreferences().getChatModel());
    assertTrue(storage.isDirty());
    verify(files, never()).write(any(), any());
    drainWork();
    realm.drain();

    ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
    verify(files).write(eq(Path.of(persistence().getPath(), "alice", "pref.json")), saved.capture());
    assertTrue(saved.getValue().contains("\"chatModel\":\"latest\""));
    assertFalse(storage.isDirty());
    assertEquals(PreferenceStorage.SaveState.SAVED, storage.getSaveStatus().getValue());
    verify(connection).persistence();
  }

  @Test
  void testPersist_WhileSameRevisionIsWriting_DoesNotDuplicateSave() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    storage.update(preference -> preference.setChatModel("chosen"));
    // Only the first write requests retries, so a duplicate cannot create an endless test loop.
    Mockito.doAnswer(invocation -> {
      storage.persist();
      storage.persist();
      Mockito.doNothing().when(files).write(any(), any());
      return null;
    }).when(files).write(any(), any());
    drainWork();

    verify(files).write(any(), any());
    assertFalse(storage.isDirty());
  }

  @Test
  void testUpdate_DuringWrite_PreservesSnapshotAndKeepsNewerRevisionDirty() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    AtomicInteger writes = new AtomicInteger();
    Mockito.doAnswer(invocation -> {
      String json = invocation.getArgument(1);
      assertTrue(storage.isDirty());
      if (writes.incrementAndGet() == 1) {
        assertTrue(json.contains("\"chatModel\":\"first\""));
        CompletableFuture.runAsync(() -> {
          storage.update(preference -> preference.setChatModel("middle"));
          storage.update(preference -> preference.setChatModel("latest"));
        }).get(5, TimeUnit.SECONDS);
        assertEquals("latest", storage.getReadyPreferences().getChatModel());
        assertTrue(json.contains("\"chatModel\":\"first\""), "Issued snapshots must be stable");
      } else {
        assertTrue(json.contains("\"chatModel\":\"latest\""));
        realm.drain();
        assertEquals(PreferenceStorage.SaveState.SAVING, storage.getSaveStatus().getValue());
      }
      return null;
    }).when(files).write(any(), any());

    storage.update(preference -> preference.setChatModel("first"));
    drainWork();
    realm.drain();

    assertEquals(2, writes.get());
    assertFalse(storage.isDirty());
    assertEquals(PreferenceStorage.SaveState.SAVED, storage.getSaveStatus().getValue());
  }

  @Test
  void testPersist_AfterFailure_RetriesNewestChoicesAndRetainedHistory() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{\"userInputs\":[\"restored\"]}");
    load();
    Mockito.doThrow(new IOException("read-only")).when(files).write(any(), any());
    storage.update(preference -> preference.setChatModel("first"));
    drainWork();
    realm.drain();
    assertTrue(storage.isDirty());
    assertEquals(PreferenceStorage.SaveState.FAILED, storage.getSaveStatus().getValue());
    storage.update(preference -> preference.setChatModel("latest"));
    drainWork();
    realm.drain();
    assertEquals(PreferenceStorage.SaveState.FAILED, storage.getSaveStatus().getValue());
    Mockito.doNothing().when(files).write(any(), any());

    storage.persist();
    storage.persist();
    drainWork();
    realm.drain();

    ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
    verify(files, times(3)).write(any(), saved.capture());
    assertTrue(saved.getValue().contains("\"chatModel\":\"latest\""));
    assertTrue(saved.getValue().contains("\"userInputs\":[\"restored\"]"));
    assertFalse(storage.isDirty());
    assertEquals(PreferenceStorage.SaveState.SAVED, storage.getSaveStatus().getValue());
    verify(connection).persistence();
  }

  @Test
  void testUpdate_AccountChangesDuringOldWrite_OldFailureCannotAffectNewSave() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    AtomicInteger writes = new AtomicInteger();
    Mockito.doAnswer(invocation -> {
      if (writes.incrementAndGet() == 1) {
        assertEquals(Path.of(persistence().getPath(), "alice", "pref.json"), invocation.getArgument(0));
        when(auth.getUserName()).thenReturn("bob");
        response = new CompletableFuture<>();
        load();
        storage.update(preference -> preference.setChatModel("bob-model"));
        throw new IOException("old account failure");
      }
      assertEquals(Path.of(persistence().getPath(), "bob", "pref.json"), invocation.getArgument(0));
      assertTrue(storage.isDirty());
      realm.drain();
      assertEquals(PreferenceStorage.SaveState.SAVING, storage.getSaveStatus().getValue());
      assertEquals("bob-model", storage.getReadyPreferences().getChatModel());
      return null;
    }).when(files).write(any(), any());

    storage.update(preference -> preference.setChatModel("alice-model"));
    drainWork();
    realm.drain();

    assertEquals(2, writes.get());
    assertFalse(storage.isDirty());
    assertEquals(PreferenceStorage.SaveState.SAVED, storage.getSaveStatus().getValue());
  }

  @Test
  void testDispose_WithAcceptedSnapshots_FinishesLatestWithoutLatePublication() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    AtomicInteger writes = new AtomicInteger();
    Mockito.doAnswer(invocation -> {
      if (writes.incrementAndGet() == 1) {
        storage.update(preference -> preference.setChatModel("latest"));
        storage.dispose();
        realm.drain();
        assertTrue(storage.getSaveStatus().isDisposed());
        assertFalse(storage.update(preference -> preference.setChatModel("rejected")));
      } else {
        assertTrue(((String) invocation.getArgument(1)).contains("\"chatModel\":\"latest\""));
      }
      return null;
    }).when(files).write(any(), any());

    storage.update(preference -> preference.setChatModel("first"));
    drainWork();
    realm.drain();

    assertEquals(2, writes.get());
    assertEquals(State.DISPOSED, storage.getState());
    assertNull(storage.getReadyPreferences());
    verify(connection).persistence();
  }

  @Test
  void testUpdate_RetainedEditAndReadSnapshots_CannotChangeSavedDocument() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    UserPreference[] retained = new UserPreference[1];
    storage.update(preference -> {
      retained[0] = preference;
      preference.setChatModel("chosen");
      preference.setUserInputs(new ArrayList<>(List.of("history")));
    });
    retained[0].setChatModel("escaped edit");
    retained[0].getUserInputs().clear();
    UserPreference read = storage.getReadyPreferences();
    read.setChatModel("escaped read");
    read.getUserInputs().clear();
    drainWork();

    ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
    verify(files).write(any(), saved.capture());
    assertTrue(saved.getValue().contains("\"chatModel\":\"chosen\""));
    assertTrue(saved.getValue().contains("\"userInputs\":[\"history\"]"));
    assertEquals("chosen", storage.getReadyPreferences().getChatModel());
    assertEquals(List.of("history"), storage.getReadyPreferences().getUserInputs());
  }

  @Test
  void testInitialize_SavedPreferences_RestoresOnlyInRealm() throws Exception {
    when(files.read(any(Path.class))).thenReturn("""
        {"chatModel":"model-a","chatModeName":"Ask","userInputs":["hello"],
         "skipGitHubJobConfirmDialog":true,"reasoningEffortByModel":{"model-a":"high"},
         "contextWindowByModel":{"model-a":128000}}
        """);

    storage.initialize();
    assertEquals(State.LOADING, storage.getState());
    assertNull(storage.getReadyPreferences());
    drainWork();
    response.complete(persistence());
    drainWork();
    assertEquals(State.LOADING, storage.getState());
    realm.drain();

    assertEquals(State.READY, storage.getReadiness().getValue());
    UserPreference restored = storage.getReadyPreferences();
    assertEquals("model-a", restored.getChatModel());
    assertEquals("Ask", restored.getChatModeName());
    assertEquals("hello", restored.getUserInputs().get(0));
    assertEquals(true, restored.isSkipGitHubJobConfirmDialog());
    assertEquals("high", restored.getReasoningEffort("model-a"));
    assertEquals(128000, restored.getContextWindow("model-a"));
    assertEquals(restored, storage.getReadyPreferences());
  }

  @Test
  void testInitialize_MissingFile_UsesFirstRunDefaultsWithoutWriting() throws Exception {
    when(files.read(any(Path.class))).thenThrow(new NoSuchFileException("pref.json"));
    load();

    assertEquals(State.READY, storage.getState());
    assertNotNull(storage.getReadyPreferences());
    assertNull(storage.getReadyPreferences().getChatModel());
    assertTrue(storage.getReadyPreferences().getReasoningEffortSnapshot().isEmpty());
    verify(files, never()).write(any(), any());
  }

  @Test
  void testInitialize_UnreadableFile_FailsAndDoesNotOverwrite() throws Exception {
    when(files.read(any(Path.class))).thenThrow(new AccessDeniedException("pref.json"));
    load();

    assertFailedWithoutWrites();
    storage.initialize();
    drainWork();
    verify(connection).persistence();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "null", "[]", "{", "{\"chatModel\":\"x\"} garbage", "{chatModel:'x'}",
      "{\"chatModel\":\"x\",}", "/*comment*/{}", "{\"chatModel\":{}}", "{\"contextWindowByModel\":false}"})
  void testInitialize_InvalidJson_FailsAndDoesNotOverwrite(String content) throws Exception {
    when(files.read(any(Path.class))).thenReturn(content);
    load();

    assertFailedWithoutWrites();
  }

  @Test
  void testInitialize_NullModelMaps_NormalizesSafeDefaults() throws Exception {
    when(files.read(any(Path.class))).thenReturn("""
        {"reasoningEffortByModel":null,"contextWindowByModel":null}
        """);
    load();

    assertEquals(State.READY, storage.getState());
    UserPreference restored = storage.getReadyPreferences();
    assertNull(restored.getReasoningEffort("model-a"));
    assertNull(restored.getContextWindow("model-a"));
    restored.setReasoningEffort("model-a", "high");
    assertTrue(restored.setContextWindow("model-a", 128000));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"chatModel\":\"line\nbreak\"}", "{\"chatModel\":\"tab\tcharacter\"}",
      "{\"chatModel\":\"escaped\\\nnewline\"}", "{\"chatModel\":\"single\\'quote\"}",
      "{\"skipGitHubJobConfirmDialog\":TRUE}", "{\"chatModel\":NULL}"})
  void testInitialize_LegacyGsonExtensions_FailWithoutOverwriting(String content) throws Exception {
    when(files.read(any(Path.class))).thenReturn(content);
    load();

    assertFailedWithoutWrites();
  }

  @Test
  void testInitialize_ValidJsonEscapes_RestoresOriginalText() throws Exception {
    when(files.read(any(Path.class))).thenReturn("""
        {"userInputs":["line\\nbreak","tab\\tcharacter","quote\\" and slash\\\\","\\u0041",
        "single'quote"],"skipGitHubJobConfirmDialog":false}
        """);
    load();

    assertEquals(State.READY, storage.getState());
    assertEquals(List.of("line\nbreak", "tab\tcharacter", "quote\" and slash\\", "A", "single'quote"),
        storage.getReadyPreferences().getUserInputs());
  }

  @Test
  void testInitialize_PendingRpc_LeavesUiPublicationAndGettersAvailable() {
    storage.initialize();
    drainWork();
    realm.drain();

    assertEquals(State.LOADING, storage.getReadiness().getValue());
    assertNull(storage.getReadyPreferences());
    assertFalse(response.isDone());
    verifyNoInteractions(files);
    verify(timer).schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS));
  }

  @Test
  void testInitialize_ConcurrentCallersAndRetry_CoalescesPendingAndReadyLoads() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    storage.initialize();
    storage.initialize();
    storage.retry();
    storage.retry();
    drainWork();
    response.complete(persistence());
    drainWork();
    realm.drain();
    storage.update(preference -> preference.setChatModel("session-choice"));
    storage.initialize();
    storage.retry();
    drainWork();

    verify(connection).persistence();
    assertEquals("session-choice", storage.getReadyPreferences().getChatModel());
  }

  @Test
  void testInitialize_ParallelCallers_ShareOnePendingRequest() throws Exception {
    CompletableFuture<?>[] callers = new CompletableFuture<?>[12];
    for (int i = 0; i < callers.length; i++) {
      callers[i] = CompletableFuture.runAsync(storage::initialize);
    }
    CompletableFuture.allOf(callers).get(5, TimeUnit.SECONDS);
    drainWork();
    realm.drain();

    assertEquals(State.LOADING, storage.getReadiness().getValue());
    assertNull(storage.getReadyPreferences());
    verify(connection).persistence();
  }

  @Test
  void testInitialize_RpcFailure_RequiresExplicitRetry() throws Exception {
    storage.initialize();
    drainWork();
    response.completeExceptionally(new IOException("offline"));
    realm.drain();
    assertFailedWithoutWrites();
    storage.initialize();
    drainWork();
    verify(connection).persistence();

    response = new CompletableFuture<>();
    when(files.read(any(Path.class))).thenReturn("{\"chatModel\":\"recovered\"}");
    storage.retry();
    storage.retry();
    drainWork();
    response.complete(persistence());
    drainWork();
    realm.drain();

    assertEquals("recovered", storage.getReadyPreferences().getChatModel());
    verify(connection, times(2)).persistence();
  }

  @Test
  void testInitialize_NullRpcResponse_Fails() throws Exception {
    storage.initialize();
    drainWork();
    response.complete(null);
    drainWork();
    realm.drain();

    assertFailedWithoutWrites();
    verify(files, never()).read(any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "relative-path", "invalid\u0000path"})
  void testInitialize_InvalidPersistencePath_Fails(String path) throws Exception {
    storage.initialize();
    drainWork();
    ChatPersistence invalid = new ChatPersistence();
    invalid.setPath(path);
    response.complete(invalid);
    drainWork();
    realm.drain();

    assertFailedWithoutWrites();
    verify(files, never()).read(any());
  }

  @Test
  void testInitialize_RpcInvocationThrows_FailsWithoutReading() throws Exception {
    when(connection.persistence()).thenThrow(new IllegalStateException("unavailable"));
    storage.initialize();
    drainWork();
    realm.drain();

    assertFailedWithoutWrites();
    verify(files, never()).read(any());
  }

  @Test
  void testInitialize_NullRpcFuture_FailsWithoutReading() throws Exception {
    when(connection.persistence()).thenReturn(null);
    storage.initialize();
    drainWork();
    realm.drain();

    assertFailedWithoutWrites();
    verify(files, never()).read(any());
  }

  @Test
  void testDeadline_PendingRpc_FailsAndCancelsRequest() throws Exception {
    storage.initialize();
    drainWork();
    expire();
    realm.drain();

    assertFailedWithoutWrites();
    assertTrue(response.isCancelled());
  }

  @Test
  void testDeadline_BeforeWorkerDispatch_DoesNotStartRpc() throws Exception {
    storage.initialize();
    expire();
    drainWork();
    realm.drain();

    assertFailedWithoutWrites();
    verify(connection, never()).persistence();
  }

  @Test
  void testDeadline_DelayedTimer_StillRejectsPublicationAfterDeadline() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    storage.initialize();
    drainWork();
    response.complete(persistence());
    drainWork();
    clock.set(TimeUnit.SECONDS.toNanos(15));
    realm.drain();

    assertFailedWithoutWrites();
  }

  @Test
  void testDeadline_DuringRead_RejectsLateReadResult() throws Exception {
    when(files.read(any(Path.class))).thenAnswer(invocation -> {
      expire();
      return "{\"chatModel\":\"too-late\"}";
    });
    load();

    assertFailedWithoutWrites();
  }

  @Test
  void testRetry_LateReadFromTimedOutAttempt_DoesNotReplaceSuccessfulRetry() throws Exception {
    when(files.read(any(Path.class))).thenAnswer(invocation -> {
      Mockito.doReturn("{\"chatModel\":\"new\"}").when(files).read(any(Path.class));
      expire();
      response = new CompletableFuture<>();
      storage.retry();
      drainWork();
      response.complete(persistence());
      drainWork();
      realm.drain();
      return "{\"chatModel\":\"old\"}";
    });
    load();

    assertEquals(State.READY, storage.getState());
    assertEquals("new", storage.getReadyPreferences().getChatModel());
    verify(connection, times(2)).persistence();
  }

  @Test
  void testInitialize_DuringFileRead_GettersRemainAvailableToOtherThreads() throws Exception {
    when(files.read(any(Path.class))).thenAnswer(invocation -> {
      assertEquals(State.LOADING, CompletableFuture.supplyAsync(storage::getState).get(5, TimeUnit.SECONDS));
      assertNull(CompletableFuture.supplyAsync(storage::getReadyPreferences).get(5, TimeUnit.SECONDS));
      return "{}";
    });
    load();

    assertEquals(State.READY, storage.getState());
  }

  @Test
  void testRetry_LateRpcFromTimedOutAttempt_CannotReplaceNewPreferences() throws Exception {
    response = new UncancellableFuture();
    CompletableFuture<ChatPersistence> old = response;
    storage.initialize();
    drainWork();
    expire();
    realm.drain();

    response = new CompletableFuture<>();
    when(files.read(any(Path.class))).thenReturn("{\"chatModel\":\"new\"}");
    storage.retry();
    drainWork();
    response.complete(persistence());
    drainWork();
    realm.drain();
    old.complete(persistence());
    drainWork();
    realm.drain();

    assertEquals(State.READY, storage.getState());
    assertEquals("new", storage.getReadyPreferences().getChatModel());
    verify(files).read(any());
    verify(files, never()).write(any(), any());
  }

  @Test
  void testInitialize_SignedOut_DoesNotReadOrResolvePath() {
    when(auth.isSignedIn()).thenReturn(false);
    storage.initialize();
    storage.retry();
    drainWork();
    realm.drain();

    assertEquals(State.UNAVAILABLE, storage.getState());
    assertEquals(State.UNAVAILABLE, storage.getReadiness().getValue());
    assertNull(storage.getReadyPreferences());
    verifyNoInteractions(connection, files);
  }

  @Test
  void testInitialize_BlankAccount_DoesNotResolvePath() {
    when(auth.getUserName()).thenReturn(" ");
    storage.initialize();
    drainWork();
    realm.drain();

    assertEquals(State.UNAVAILABLE, storage.getState());
    verifyNoInteractions(connection, files);
  }

  @Test
  void testAccountChange_ReadyPreferences_AreInaccessibleBeforeRealmCallback() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{\"chatModel\":\"alice-model\"}");
    load();
    when(auth.getUserName()).thenReturn("bob");

    assertNull(storage.getReadyPreferences());
    assertEquals(State.UNAVAILABLE, storage.getState());
    storage.persist();
    realm.drain();
    assertEquals(State.UNAVAILABLE, storage.getReadiness().getValue());
    verify(files, never()).write(any(), any());
    verify(connection).persistence();
  }

  @Test
  void testAccountChange_QueuedRestoration_CannotPublishOldPreferences() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{\"chatModel\":\"alice-model\"}");
    storage.initialize();
    drainWork();
    response.complete(persistence());
    drainWork();
    when(auth.getUserName()).thenReturn("bob");
    realm.drain();

    assertEquals(State.UNAVAILABLE, storage.getReadiness().getValue());
    assertNull(storage.getReadyPreferences());
    verify(connection).persistence();
  }

  @Test
  void testAccountChange_LateOldRpc_DoesNotAffectNewAccount() throws Exception {
    response = new UncancellableFuture();
    CompletableFuture<ChatPersistence> old = response;
    storage.initialize();
    drainWork();
    when(auth.getUserName()).thenReturn("bob");
    authListener().onDidCopilotStatusChange(new CopilotStatusResult());
    realm.drain();
    assertEquals(State.UNAVAILABLE, storage.getState());
    verify(connection).persistence();

    response = new CompletableFuture<>();
    when(files.read(any(Path.class))).thenReturn("{\"chatModel\":\"bob-model\"}");
    storage.retry();
    drainWork();
    response.complete(persistence());
    drainWork();
    realm.drain();
    old.complete(persistence());
    drainWork();
    realm.drain();

    assertEquals("bob-model", storage.getReadyPreferences().getChatModel());
    verify(files).read(Path.of(persistence().getPath(), "bob", "pref.json"));
    verify(files, never()).read(Path.of(persistence().getPath(), "alice", "pref.json"));
  }

  @Test
  void testAuthNotification_SignOut_InvalidatesWithoutAutomaticRetry() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    when(auth.isSignedIn()).thenReturn(false);
    authListener().onDidCopilotStatusChange(new CopilotStatusResult());
    assertNull(storage.getReadyPreferences());
    realm.drain();
    assertEquals(State.UNAVAILABLE, storage.getReadiness().getValue());

    when(auth.isSignedIn()).thenReturn(true);
    authListener().onDidCopilotStatusChange(new CopilotStatusResult());
    drainWork();
    realm.drain();
    verify(connection).persistence();
    assertEquals(State.UNAVAILABLE, storage.getState());
  }

  @Test
  void testAuthNotification_SameAccount_DoesNotResetReadySessionChoices() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    storage.update(preference -> preference.setChatModel("session-choice"));
    authListener().onDidCopilotStatusChange(new CopilotStatusResult());
    drainWork();
    realm.drain();

    assertEquals("session-choice", storage.getReadyPreferences().getChatModel());
    verify(connection).persistence();
  }

  @Test
  void testDispose_PendingRpc_DetachesListenerAndRejectsLateResults() throws Exception {
    response = new UncancellableFuture();
    storage.initialize();
    drainWork();
    CopilotAuthStatusListener listener = authListener();
    storage.dispose();
    storage.dispose();
    storage.initialize();
    storage.retry();
    response.complete(persistence());
    drainWork();
    realm.drain();

    assertEquals(State.DISPOSED, storage.getState());
    assertNull(storage.getReadyPreferences());
    assertTrue(storage.getReadiness().isDisposed());
    verify(auth).removeCopilotAuthStatusListener(listener);
    verify(worker).shutdown();
    verify(timer).shutdownNow();
    verify(connection).persistence();
    verifyNoInteractions(files);
  }

  @Test
  void testDispose_QueuedRestoration_DoesNotPublishReady() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    storage.initialize();
    drainWork();
    response.complete(persistence());
    drainWork();
    storage.dispose();
    realm.drain();

    assertEquals(State.DISPOSED, storage.getState());
    assertNull(storage.getReadyPreferences());
    assertTrue(storage.getReadiness().isDisposed());
  }

  @Test
  void testPersist_ReadyPreferences_UsesResolvedAccountPathWithoutRpc() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    storage.update(preference -> preference.setChatModel("saved-model"));
    storage.persist();
    drainWork();

    ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
    verify(files).write(eq(Path.of(persistence().getPath(), "alice", "pref.json")), saved.capture());
    assertTrue(saved.getValue().contains("\"chatModel\":\"saved-model\""));
    verify(connection).persistence();
  }

  @Test
  void testUpdate_DetachedOldAccountSnapshot_CannotChangeNewAccount() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    UserPreference original = storage.getReadyPreferences();
    when(auth.getUserName()).thenReturn("bob");
    response = new CompletableFuture<>();
    load();

    original.setChatModel("old-account");
    storage.persist();
    verify(files, never()).write(any(), any());
    storage.update(preference -> preference.setChatModel("bob-model"));
    drainWork();

    ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
    verify(files).write(eq(Path.of(persistence().getPath(), "bob", "pref.json")), saved.capture());
    assertTrue(saved.getValue().contains("\"chatModel\":\"bob-model\""));
    verify(connection, times(2)).persistence();
  }

  @Test
  void testUpdate_DetachedSnapshot_CannotMutateAuthoritativePreferences() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    UserPreference unrelated = new UserPreference();
    assertEquals(storage.getReadyPreferences(), unrelated);

    storage.getReadyPreferences().setChatModel("detached");
    storage.persist();
    drainWork();

    verify(files, never()).write(any(), any());
    assertNull(storage.getReadyPreferences().getChatModel());
  }

  @Test
  void testPersist_DisposedPreference_DoesNotWrite() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    storage.dispose();

    storage.persist();

    verify(files, never()).write(any(), any());
  }

  @Test
  void testPersist_DuringFileWrite_GettersRemainAvailableToOtherThreads() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    Mockito.doAnswer(invocation -> {
      assertEquals(State.READY, CompletableFuture.supplyAsync(storage::getState).get(5, TimeUnit.SECONDS));
      assertEquals("saved", CompletableFuture.supplyAsync(storage::getReadyPreferences)
          .get(5, TimeUnit.SECONDS).getChatModel());
      return null;
    }).when(files).write(any(), any());

    storage.update(preference -> preference.setChatModel("saved"));
    drainWork();

    verify(files).write(any(), any());
  }

  @Test
  void testPersist_AccountChangesDuringWrite_UsesOnlyCapturedAccountPath() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    Mockito.doAnswer(invocation -> {
      when(auth.getUserName()).thenReturn("bob");
      assertNull(storage.getReadyPreferences());
      return null;
    }).when(files).write(any(), any());
    storage.update(preference -> preference.setChatModel("alice-model"));
    drainWork();

    verify(files).write(eq(Path.of(persistence().getPath(), "alice", "pref.json")), any());
    assertEquals(State.UNAVAILABLE, storage.getState());
    verify(connection).persistence();
  }

  @Test
  void testPersist_WriteFailure_PreservesReadySessionChoices() throws Exception {
    when(files.read(any(Path.class))).thenReturn("{}");
    load();
    storage.update(preference -> preference.setChatModel("session-choice"));
    Mockito.doThrow(new IOException("read-only")).when(files).write(any(), any());
    storage.persist();
    drainWork();

    assertEquals(State.READY, storage.getState());
    assertEquals("session-choice", storage.getReadyPreferences().getChatModel());
  }

  @Test
  void testPersist_StillLoading_DoesNotWriteDefaultsOrResolveAgain() throws Exception {
    storage.initialize();
    drainWork();
    storage.persist();

    assertEquals(State.LOADING, storage.getState());
    verifyNoInteractions(files);
    verify(connection).persistence();
  }

  private CopilotAuthStatusListener authListener() {
    ArgumentCaptor<CopilotAuthStatusListener> listener = ArgumentCaptor.forClass(CopilotAuthStatusListener.class);
    verify(auth).addCopilotAuthStatusListener(listener.capture());
    return listener.getValue();
  }

  private void expire() {
    clock.addAndGet(TimeUnit.SECONDS.toNanos(15));
    deadlines.remove().run();
  }

  private void load() {
    storage.initialize();
    drainWork();
    response.complete(persistence());
    drainWork();
    realm.drain();
  }

  private void assertFailedWithoutWrites() throws IOException {
    assertEquals(State.FAILED, storage.getState());
    assertEquals(State.FAILED, storage.getReadiness().getValue());
    assertNull(storage.getReadyPreferences());
    storage.persist();
    verify(files, never()).write(any(), any());
  }

  private ChatPersistence persistence() {
    ChatPersistence result = new ChatPersistence();
    result.setPath(Path.of("preferences").toAbsolutePath().toString());
    return result;
  }

  private void drainWork() {
    while (!work.isEmpty()) {
      work.remove().run();
    }
  }

  private static class TestRealm extends Realm {
    private final Queue<Runnable> publications = new ConcurrentLinkedQueue<>();

    @Override
    public boolean isCurrent() {
      return true;
    }

    @Override
    public void asyncExec(Runnable runnable) {
      publications.add(runnable);
    }

    void drain() {
      while (!publications.isEmpty()) {
        publications.remove().run();
      }
    }
  }

  private static class UncancellableFuture extends CompletableFuture<ChatPersistence> {
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }
  }

  // A public concrete boundary adapter supports Mockito's inline mock maker across OSGi class loaders.
  public static class TestFileAccess implements PreferenceStorage.FileAccess {
    @Override
    public String read(Path path) throws IOException {
      return Files.readString(path);
    }

    @Override
    public void write(Path path, String content) throws IOException {
      Files.writeString(path, content);
    }
  }
}
