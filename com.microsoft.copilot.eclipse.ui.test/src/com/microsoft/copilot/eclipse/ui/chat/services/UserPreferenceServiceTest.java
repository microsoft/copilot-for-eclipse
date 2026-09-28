// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.timeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.SWT;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.ui.swt.DropdownButton;
import com.microsoft.copilot.eclipse.ui.chat.Messages;
import com.microsoft.copilot.eclipse.ui.chat.PreferenceStatus;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {
  @Mock
  private CopilotLanguageServerConnection connection;
  @Mock
  private AuthStatusManager auth;
  @TempDir
  private Path directory;

  private PreferenceStorage storage;
  private UserPreferenceService service;
  private Shell shell;
  private DropdownButton picker;
  private PreferenceStatus status;

  @BeforeEach
  void setUp() {
    lenient().when(connection.listConversationModes(any()))
        .thenReturn(CompletableFuture.completedFuture(new ConversationMode[] {
            builtInMode("Ask", "Ask"), builtInMode("Agent", "Agent"), builtInMode("Plan", "Plan")}));
  }

  @AfterEach
  void tearDown() {
    runOnUi(() -> {
      if (shell != null) {
        shell.dispose();
      }
      if (service != null) {
        service.dispose();
      }
      if (storage != null) {
        storage.dispose();
      }
    });
  }

  @Test
  void testFirstInitialization_PendingModeDiscovery_ProcessesSwtAndPublishesModesAfterCompletion() throws Exception {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("user");
    CompletableFuture<ChatPersistence> preferences = new CompletableFuture<>();
    CompletableFuture<ConversationMode[]> modes = new CompletableFuture<>();
    when(connection.persistence()).thenReturn(preferences);
    when(connection.listConversationModes(any())).thenReturn(modes);
    writePreferences("{\"chatModeName\":\"Ask\"}");
    CompletableFuture<Boolean> uiAction = new CompletableFuture<>();
    Display.getDefault().asyncExec(() -> {
      try {
        storage = new PreferenceStorage(connection, auth);
        createControls();
        Display.getDefault().asyncExec(() -> {
          try {
            uiAction.complete(!picker.getEnabled() && storage.getState() == PreferenceStorage.State.LOADING);
          } catch (Throwable error) {
            uiAction.completeExceptionally(error);
          }
        });
      } catch (Throwable error) {
        uiAction.completeExceptionally(error);
      }
    });
    try {
      assertTrue(uiAction.get(5, TimeUnit.SECONDS), "Cold mode discovery must not prevent SWT processing");
      verify(connection, timeout(5000)).listConversationModes(any());
      assertFalse(modes.isDone());
      assertFalse(preferences.isDone());
      ConversationMode ask = new ConversationMode();
      ask.setId("pending-discovery-ask");
      ask.setName("Ask");
      ask.setKind("Ask");
      ask.setBuiltIn(true);
      modes.complete(new ConversationMode[] {ask});
      preferences.complete(persistence());
      awaitUi(() -> picker.getEnabled() && "Ask".equals(picker.getSelectedItemId())
          && BuiltInChatModeManager.INSTANCE.getBuiltInModeById("pending-discovery-ask") != null);
      runOnUi(() -> assertEquals(ChatMode.Ask, service.getActiveChatMode()));
    } finally {
      modes.complete(new ConversationMode[0]);
      preferences.completeExceptionally(new IllegalStateException("test cleanup"));
    }
  }

  @Test
  void testInitialization_PreferencesBeforeModeDiscovery_GatesPlanActionsAndRefreshesConsumers() throws Exception {
    CompletableFuture<ConversationMode[]> modes = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(modes);
    writePreferences("{\"chatModeName\":\"Plan\"}");
    AtomicBoolean actionsReady = new AtomicBoolean();
    AtomicBoolean resolvedModeNotification = new AtomicBoolean();
    AtomicInteger notifications = new AtomicInteger();
    AtomicReference<ISideEffect> readinessBinding = new AtomicReference<>();
    IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    EventHandler listener = event -> {
      notifications.incrementAndGet();
      if (BuiltInChatModeManager.INSTANCE.getBuiltInModeById("delayed-plan") != null) {
        resolvedModeNotification.set(event.getProperty(IEventBroker.DATA) == ChatMode.Agent);
      }
    };
    broker.subscribe(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, listener);
    try {
      startAuthenticated(CompletableFuture.completedFuture(persistence()));
      awaitUi(() -> storage.getState() == PreferenceStorage.State.READY && notifications.get() > 0);
      runOnUi(() -> {
        Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), () -> readinessBinding.set(
            ISideEffect.create(service::isActiveModeReady, actionsReady::set)));
        assertEquals("Plan", service.getActiveModeNameOrId());
        assertFalse(service.isActiveModeReady(), "Unresolved Plan must not be sent as Agent");
      });
      assertFalse(actionsReady.get());

      modes.complete(new ConversationMode[] {builtInMode("delayed-plan", "Plan")});
      awaitUi(() -> actionsReady.get() && resolvedModeNotification.get());
      runOnUi(() -> {
        assertEquals("Plan", service.getActiveModeNameOrId());
        assertEquals("delayed-plan",
            BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName("Plan").getId());
      });
    } finally {
      broker.unsubscribe(listener);
      runOnUi(() -> {
        if (readinessBinding.get() != null) {
          readinessBinding.get().dispose();
        }
      });
      modes.complete(new ConversationMode[0]);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testModeDiscovery_FailedOrEmpty_ShowsRetryWithoutReloadingReadyPreferences(boolean empty) throws Exception {
    CompletableFuture<ConversationMode[]> initial = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(initial);
    Path file = writePreferences("{\"chatModeName\":\"Plan\"}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Plan".equals(service.getActiveModeNameOrId()));
    UserPreference restored = storage.getReadyPreferences();
    runOnUi(() -> {
      assertTrue(status.getVisible());
      assertEquals(Messages.modeDiscoveryLoading, statusMessage().getText());
      assertFalse(service.isActiveModeReady());
    });
    if (empty) {
      initial.complete(new ConversationMode[0]);
    } else {
      initial.completeExceptionally(new IllegalStateException("mode discovery failed"));
    }
    awaitUi(() -> Messages.modeDiscoveryFailed.equals(statusMessage().getText()));

    CompletableFuture<ConversationMode[]> retry = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(retry);
    runOnUi(() -> {
      assertEquals(PreferenceStorage.State.READY, storage.getState());
      assertTrue(status.getVisible());
      Link retryLink = Arrays.stream(status.getChildren()).filter(Link.class::isInstance)
          .map(Link.class::cast).findFirst().orElseThrow();
      assertTrue(retryLink.getEnabled());
      retryLink.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
      service.retryModeDiscovery();
      service.retryModeDiscovery();
      assertFalse(service.isActiveModeReady());
    });
    awaitUi(() -> Messages.modeDiscoveryLoading.equals(statusMessage().getText()));
    runOnUi(() -> {
      Link retryLink = Arrays.stream(status.getChildren()).filter(Link.class::isInstance)
          .map(Link.class::cast).findFirst().orElseThrow();
      assertFalse(retryLink.getEnabled());
    });
    verify(connection, timeout(5000).times(2)).listConversationModes(any());
    retry.complete(new ConversationMode[] {builtInMode("recovered-plan", "Plan")});
    awaitUi(() -> service.isActiveModeReady() && !status.getVisible());
    runOnUi(() -> {
      assertEquals(restored, storage.getReadyPreferences());
      assertEquals("Plan", service.getActiveModeNameOrId());
      assertEquals("recovered-plan",
          BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName("Plan").getId());
    });
    verify(connection).persistence();
    assertEquals("{\"chatModeName\":\"Plan\"}", Files.readString(file));
  }

  @Test
  void testModeDiscovery_SupersededAttempt_CannotReplaceRecoveredInventory() throws Exception {
    CompletableFuture<ConversationMode[]> obsolete = new CompletableFuture<>();
    CompletableFuture<ConversationMode[]> current = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(obsolete, current);
    writePreferences("{\"chatModeName\":\"Plan\"}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Plan".equals(service.getActiveModeNameOrId()));
    verify(connection, timeout(5000)).listConversationModes(any());
    UserPreference restored = storage.getReadyPreferences();

    CopilotStatusResult statusResult = new CopilotStatusResult();
    statusResult.setStatus(CopilotStatusResult.OK);
    statusResult.setUser("user");
    IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    runOnUi(() -> {
      broker.send(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, statusResult);
      service.retryModeDiscovery();
    });
    verify(connection, timeout(5000).times(2)).listConversationModes(any());
    current.complete(new ConversationMode[] {builtInMode("current-plan", "Plan")});
    awaitUi(() -> service.isActiveModeReady() && !status.getVisible());
    obsolete.complete(new ConversationMode[] {builtInMode("obsolete-plan", "Plan")});
    runOnUi(() -> {
      assertEquals("current-plan", BuiltInChatModeManager.INSTANCE.getBuiltInModeByDisplayName("Plan").getId());
      assertEquals(UserPreferenceService.ModeDiscoveryState.READY, service.getModeDiscoveryState());
      assertEquals(restored, storage.getReadyPreferences());
    });
    verify(connection).persistence();
  }

  @Test
  void testModeDiscovery_DisposedDuringManualRetry_RejectsLateSuccess() throws Exception {
    when(connection.listConversationModes(any()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> service.getModeDiscoveryState() == UserPreferenceService.ModeDiscoveryState.FAILED);
    CompletableFuture<ConversationMode[]> retry = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(retry);
    runOnUi(service::retryModeDiscovery);
    verify(connection, timeout(5000).times(2)).listConversationModes(any());
    runOnUi(() -> {
      shell.dispose();
      service.dispose();
    });
    retry.complete(new ConversationMode[] {builtInMode("disposed-retry-mode", "Plan")});
    runOnUi(() -> {
      assertFalse(service.isActiveModeReady());
      assertEquals(UserPreferenceService.ModeDiscoveryState.UNAVAILABLE, service.getModeDiscoveryState());
      assertNull(BuiltInChatModeManager.INSTANCE.getBuiltInModeById("disposed-retry-mode"));
    });
  }

  @Test
  void testModeDiscovery_AccountChanges_RejectsOldAccountModes() throws Exception {
    CompletableFuture<ConversationMode[]> modes = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(modes);
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    verify(connection, timeout(5000)).listConversationModes(any());
    awaitUi(() -> picker.getEnabled());

    when(auth.getUserName()).thenReturn("other-user");
    modes.complete(new ConversationMode[] {builtInMode("obsolete-account-mode", "Ask")});
    runOnUi(() -> {
      assertNull(BuiltInChatModeManager.INSTANCE.getBuiltInModeById("obsolete-account-mode"));
      assertNull(service.getActiveModeNameOrId());
    });
  }

  @Test
  void testModeDiscovery_DisposedService_DoesNotPublishLateModes() throws Exception {
    CompletableFuture<ConversationMode[]> modes = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(modes);
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    verify(connection, timeout(5000)).listConversationModes(any());
    runOnUi(() -> {
      shell.dispose();
      service.dispose();
    });

    modes.complete(new ConversationMode[] {builtInMode("disposed-service-mode", "Ask")});
    runOnUi(() ->
        assertNull(BuiltInChatModeManager.INSTANCE.getBuiltInModeById("disposed-service-mode")));
  }

  @Test
  void testModeDiscovery_AgentPolicyDisabled_KeepsAskViewAfterDelayedDiscovery() throws Exception {
    CompletableFuture<ConversationMode[]> modes = new CompletableFuture<>();
    when(connection.listConversationModes(any())).thenReturn(modes);
    writePreferences("{\"chatModeName\":\"Agent\"}");
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean original = flags.isAgentModeEnabled();
    try {
      flags.setAgentModeEnabled(false);
      startAuthenticated(CompletableFuture.completedFuture(persistence()));
      verify(connection, timeout(5000)).listConversationModes(any());
      awaitUi(() -> picker.getEnabled());
      modes.complete(new ConversationMode[] {
          builtInMode("delayed-policy-agent", "Agent"), builtInMode("delayed-policy-ask", "Ask")});
      awaitUi(() -> BuiltInChatModeManager.INSTANCE.getBuiltInModeById("delayed-policy-ask") != null);
      runOnUi(() -> assertEquals(ChatMode.Ask, service.getActiveChatMode()));
    } finally {
      flags.setAgentModeEnabled(original);
    }
  }

  @Test
  void testInitialization_RestoresModeHistoryAndConfirmationWithoutSaving() throws Exception {
    String saved = """
        {"chatModeName":"Ask","userInputs":["first","second"],"skipGitHubJobConfirmDialog":true}
        """;
    Path file = writePreferences(saved);
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()) && !status.getVisible());
    runOnUi(() -> {
      assertEquals(ChatMode.Ask, service.getActiveChatMode());
      assertEquals("second", service.getPreviousInput(""));
      assertEquals("first", service.getPreviousInput(""));
      assertTrue(service.isSkipGitHubJobConfirmDialog());
      assertTrue(picker.getEnabled());
      assertFalse(status.getVisible());
    });
    assertEquals(saved, Files.readString(file), "Restoration is not a user edit");
  }

  @Test
  void testInitialization_AgentPolicyDisabled_RestoresAskView() throws Exception {
    writePreferences("{\"chatModeName\":\"Agent\"}");
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean original = flags.isAgentModeEnabled();
    try {
      flags.setAgentModeEnabled(false);
      startAuthenticated(CompletableFuture.completedFuture(persistence()));
      awaitUi(() -> "Agent".equals(service.getActiveModeNameOrId()));
      runOnUi(() -> assertEquals(ChatMode.Ask, service.getActiveChatMode()));
    } finally {
      flags.setAgentModeEnabled(original);
    }
  }

  @Test
  void testRetry_FailedLoadKeepsPickerDisabledThenRestoresSavedChoice() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\"}");
    startAuthenticated(CompletableFuture.failedFuture(new IllegalStateException("offline")));
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.FAILED);
    runOnUi(() -> {
      assertFalse(picker.getEnabled());
      assertTrue(status.getVisible());
      Label message = Arrays.stream(status.getChildren()).filter(Label.class::isInstance)
          .map(Label.class::cast).findFirst().orElseThrow();
      assertEquals(Messages.preferenceLoadFailed, message.getText());
      service.setActiveChatMode("Agent");
      assertNull(service.getActiveModeNameOrId());
    });
    when(connection.persistence()).thenReturn(CompletableFuture.completedFuture(persistence()));
    runOnUi(() -> {
      Link retry = Arrays.stream(status.getChildren()).filter(Link.class::isInstance)
          .map(Link.class::cast).findFirst().orElseThrow();
      assertTrue(retry.getEnabled());
      retry.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
    });
    awaitUi(() -> picker.getEnabled() && "Ask".equals(service.getActiveModeNameOrId()));
  }

  @Test
  void testSignOut_InvalidatesLoadedHistoryAndDisablesPicker() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\",\"userInputs\":[\"private history\"]}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()));
    ArgumentCaptor<CopilotAuthStatusListener> listener = ArgumentCaptor.forClass(CopilotAuthStatusListener.class);
    verify(auth).addCopilotAuthStatusListener(listener.capture());
    when(auth.isSignedIn()).thenReturn(false);
    CopilotStatusResult signedOut = new CopilotStatusResult();
    signedOut.setStatus(CopilotStatusResult.NOT_SIGNED_IN);
    listener.getValue().onDidCopilotStatusChange(signedOut);
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.UNAVAILABLE);
    runOnUi(() -> {
      assertFalse(picker.getEnabled());
      assertNull(service.getActiveModeNameOrId());
      assertEquals("", service.getPreviousInput(""));
    });
  }

  @Test
  void testModeChange_DisallowedOrUnknownMode_DoesNotAlterReadyChoices() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\"}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()) && !status.getVisible());
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean original = flags.isAgentModeEnabled();
    try {
      flags.setAgentModeEnabled(false);
      runOnUi(() -> {
        service.setActiveChatMode("Agent");
        assertEquals("Ask", service.getActiveModeNameOrId());
        assertFalse(storage.isDirty());
      });
      flags.setAgentModeEnabled(true);
      runOnUi(() -> {
        service.setActiveChatMode("missing-mode");
        assertEquals("Ask", service.getActiveModeNameOrId());
        assertFalse(storage.isDirty());
      });
    } finally {
      flags.setAgentModeEnabled(original);
    }
  }

  @Test
  void testModeAndConfirmationChanges_DelayedFailedSave_StayImmediateAndRetryLatest() throws Exception {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("user");
    when(connection.persistence()).thenReturn(CompletableFuture.completedFuture(persistence()));
    writePreferences("{\"chatModeName\":\"Ask\"}");
    CountDownLatch writeStarted = new CountDownLatch(1);
    CountDownLatch releaseWrite = new CountDownLatch(1);
    AtomicBoolean failWrites = new AtomicBoolean(true);
    AtomicBoolean eventReceived = new AtomicBoolean();
    IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    EventHandler listener = event -> {
      if (event.getProperty(IEventBroker.DATA) == ChatMode.Agent) {
        eventReceived.set(true);
      }
    };
    broker.subscribe(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, listener);
    try {
      runOnUi(() -> {
        storage = new PreferenceStorage(connection, auth, DisplayRealm.getRealm(Display.getDefault()),
            Executors.newSingleThreadExecutor(), Executors.newSingleThreadScheduledExecutor(), System::nanoTime,
            new PreferenceStorage.FileAccess() {
              @Override
              public String read(Path path) throws IOException {
                return Files.readString(path);
              }

              @Override
              public void write(Path path, String content) throws IOException {
                writeStarted.countDown();
                try {
                  if (!releaseWrite.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Test did not release delayed write");
                  }
                } catch (InterruptedException exception) {
                  Thread.currentThread().interrupt();
                  throw new IOException(exception);
                }
                if (failWrites.get()) {
                  throw new IOException("read-only");
                }
                new PreferenceFileAccess().write(path, content);
              }
            });
        createControls();
      });
      awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()) && !status.getVisible());
      CompletableFuture<Void> uiAction = new CompletableFuture<>();
      Display.getDefault().asyncExec(() -> {
        try {
          service.setActiveChatMode("Agent");
          service.setSkipGitHubJobConfirmDialog(true);
          service.addInputToHistory("latest input");
          service.getPreviousInput("draft input");
          assertEquals("Agent", service.getActiveModeNameOrId());
          assertEquals("Agent", storage.getReadyPreferences().getChatModeName());
          assertTrue(service.isSkipGitHubJobConfirmDialog());
          assertEquals(Arrays.asList("latest input", "draft input"), storage.getReadyPreferences().getUserInputs());
          assertTrue(storage.isDirty());
          Display.getDefault().asyncExec(() -> uiAction.complete(null));
        } catch (Throwable error) {
          uiAction.completeExceptionally(error);
        }
      });
      uiAction.get(5, TimeUnit.SECONDS);
      assertTrue(writeStarted.await(5, TimeUnit.SECONDS));
      awaitUi(eventReceived::get);
      releaseWrite.countDown();
      awaitUi(() -> storage.getSaveStatus().getValue() == PreferenceStorage.SaveState.FAILED);
      runOnUi(() -> {
        assertTrue(status.getVisible(), "Save failures must be visible without disabling current choices");
        assertEquals(Messages.preferenceSaveFailed, statusMessage().getText());
        assertTrue(picker.getEnabled());
        assertEquals("Agent", service.getActiveModeNameOrId());
      });
      failWrites.set(false);
      runOnUi(() -> {
        Link retry = Arrays.stream(status.getChildren()).filter(Link.class::isInstance)
            .map(Link.class::cast).findFirst().orElseThrow();
        assertTrue(retry.getVisible());
        retry.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
      });
      awaitUi(() -> !storage.isDirty() && !status.getVisible());
      String persisted = Files.readString(directory.resolve("user").resolve("pref.json"));
      assertTrue(persisted.contains("\"chatModeName\":\"Agent\""));
      assertTrue(persisted.contains("\"skipGitHubJobConfirmDialog\":true"));
      assertTrue(persisted.contains("latest input"));
      assertTrue(persisted.contains("draft input"));
      verify(connection).persistence();
    } finally {
      releaseWrite.countDown();
      broker.unsubscribe(listener);
    }
  }

  @Test
  void testModeChange_AfterRestoration_UpdatesSharedStateAndPersists() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\"}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()));
    runOnUi(() -> {
      service.setActiveChatMode("Agent");
      assertEquals("Agent", service.getActiveModeNameOrId());
      assertNotNull(storage.getReadyPreferences());
      assertEquals("Agent", storage.getReadyPreferences().getChatModeName());
    });
    awaitUi(() -> !storage.isDirty());
    assertTrue(Files.readString(directory.resolve("user").resolve("pref.json")).contains("Agent"));
  }

  @Test
  void testInitialization_DeadlineExpires_ShowsFailureAndRetryWithoutEnablingPicker() throws Exception {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("user");
    CompletableFuture<ChatPersistence> pending = new CompletableFuture<>();
    when(connection.persistence()).thenReturn(pending);
    ScheduledExecutorService timer = mock(ScheduledExecutorService.class);
    AtomicReference<Runnable> deadline = new AtomicReference<>();
    when(timer.schedule(any(Runnable.class), eq(15L), eq(TimeUnit.SECONDS))).thenAnswer(invocation -> {
      deadline.set(invocation.getArgument(0));
      return mock(ScheduledFuture.class);
    });
    AtomicLong clock = new AtomicLong();
    runOnUi(() -> {
      storage = new PreferenceStorage(connection, auth, DisplayRealm.getRealm(Display.getDefault()),
          Executors.newSingleThreadExecutor(), timer, clock::get, new PreferenceStorage.FileAccess() {
            @Override
            public String read(Path path) throws IOException {
              return Files.readString(path);
            }

            @Override
            public void write(Path path, String content) throws IOException {
              Files.writeString(path, content);
            }
          });
      createControls();
    });
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.LOADING);
    runOnUi(() -> assertFalse(picker.getEnabled()));
    clock.set(TimeUnit.SECONDS.toNanos(15));
    deadline.get().run();
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.FAILED);
    runOnUi(() -> {
      assertFalse(picker.getEnabled());
      assertTrue(status.getVisible());
      Link retry = Arrays.stream(status.getChildren()).filter(Link.class::isInstance)
          .map(Link.class::cast).findFirst().orElseThrow();
      assertTrue(retry.getEnabled());
    });
  }

  private void startAuthenticated(CompletableFuture<ChatPersistence> rpc) {
    when(auth.isSignedIn()).thenReturn(true);
    when(auth.getUserName()).thenReturn("user");
    when(connection.persistence()).thenReturn(rpc);
    runOnUi(() -> {
      storage = new PreferenceStorage(connection, auth);
      createControls();
    });
  }

  private void createControls() {
    service = new UserPreferenceService(connection, auth, storage);
    shell = new Shell(Display.getDefault());
    status = new PreferenceStatus(shell, storage, service);
    picker = new DropdownButton(shell, SWT.NONE);
    service.bindChatModePicker(picker);
  }

  private ChatPersistence persistence() {
    ChatPersistence result = new ChatPersistence();
    result.setPath(directory.toString());
    return result;
  }

  private Label statusMessage() {
    return Arrays.stream(status.getChildren()).filter(Label.class::isInstance)
        .map(Label.class::cast).findFirst().orElseThrow();
  }

  private static ConversationMode builtInMode(String id, String name) {
    ConversationMode mode = new ConversationMode();
    mode.setId(id);
    mode.setName(name);
    mode.setKind(name);
    mode.setBuiltIn(true);
    return mode;
  }

  private Path writePreferences(String content) throws Exception {
    Path file = directory.resolve("user").resolve("pref.json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
    return file;
  }

  private static void runOnUi(Runnable action) {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      try {
        action.run();
      } catch (Throwable error) {
        failure.set(error);
      }
    });
    if (failure.get() != null) {
      fail("UI action failed", failure.get());
    }
  }

  private static void awaitUi(BooleanSupplier condition) throws InterruptedException {
    AtomicBoolean complete = new AtomicBoolean();
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    do {
      runOnUi(() -> complete.set(condition.getAsBoolean()));
      if (complete.get()) {
        return;
      }
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    assertTrue(complete.get(), "Timed out waiting for the preference UI");
  }
}
