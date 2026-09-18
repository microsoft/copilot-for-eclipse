// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.eclipse.swt.SWT;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
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

  @AfterEach
  void tearDown() {
    Display.getDefault().syncExec(() -> {
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
  void testInitialization_RestoresModeHistoryAndConfirmationWithoutSaving() throws Exception {
    String saved = """
        {"chatModeName":"Ask","userInputs":["first","second"],"skipGitHubJobConfirmDialog":true}
        """;
    Path file = writePreferences(saved);
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()));
    Display.getDefault().syncExec(() -> {
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
      Display.getDefault().syncExec(() -> assertEquals(ChatMode.Ask, service.getActiveChatMode()));
    } finally {
      flags.setAgentModeEnabled(original);
    }
  }

  @Test
  void testRetry_FailedLoadKeepsPickerDisabledThenRestoresSavedChoice() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\"}");
    startAuthenticated(CompletableFuture.failedFuture(new IllegalStateException("offline")));
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.FAILED);
    Display.getDefault().syncExec(() -> {
      assertFalse(picker.getEnabled());
      assertTrue(status.getVisible());
      Label message = Arrays.stream(status.getChildren()).filter(Label.class::isInstance)
          .map(Label.class::cast).findFirst().orElseThrow();
      assertEquals(Messages.preferenceLoadFailed, message.getText());
      service.setActiveChatMode("Agent");
      assertNull(service.getActiveModeNameOrId());
    });
    when(connection.persistence()).thenReturn(CompletableFuture.completedFuture(persistence()));
    Display.getDefault().syncExec(() -> {
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
    Display.getDefault().syncExec(() -> {
      assertFalse(picker.getEnabled());
      assertNull(service.getActiveModeNameOrId());
      assertEquals("", service.getPreviousInput(""));
    });
  }

  @Test
  void testModeChange_AfterRestoration_UpdatesSharedStateAndPersists() throws Exception {
    writePreferences("{\"chatModeName\":\"Ask\"}");
    startAuthenticated(CompletableFuture.completedFuture(persistence()));
    awaitUi(() -> "Ask".equals(service.getActiveModeNameOrId()));
    Display.getDefault().syncExec(() -> {
      service.setActiveChatMode("Agent");
      assertEquals("Agent", service.getActiveModeNameOrId());
      assertNotNull(storage.getReadyPreferences());
      assertEquals("Agent", storage.getReadyPreferences().getChatModeName());
    });
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
    Display.getDefault().syncExec(() -> {
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
    Display.getDefault().syncExec(() -> assertFalse(picker.getEnabled()));
    clock.set(TimeUnit.SECONDS.toNanos(15));
    deadline.get().run();
    awaitUi(() -> storage.getReadiness().getValue() == PreferenceStorage.State.FAILED);
    Display.getDefault().syncExec(() -> {
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
    Display.getDefault().syncExec(() -> {
      storage = new PreferenceStorage(connection, auth);
      createControls();
    });
  }

  private void createControls() {
    service = new UserPreferenceService(connection, auth, storage);
    shell = new Shell(Display.getDefault());
    status = new PreferenceStatus(shell, storage);
    picker = new DropdownButton(shell, SWT.NONE);
    service.bindChatModePicker(picker);
  }

  private ChatPersistence persistence() {
    ChatPersistence result = new ChatPersistence();
    result.setPath(directory.toString());
    return result;
  }

  private Path writePreferences(String content) throws Exception {
    Path file = directory.resolve("user").resolve("pref.json");
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
    return file;
  }

  private static void awaitUi(BooleanSupplier condition) throws InterruptedException {
    AtomicBoolean complete = new AtomicBoolean();
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    do {
      Display.getDefault().syncExec(() -> complete.set(condition.getAsBoolean()));
      if (complete.get()) {
        return;
      }
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    assertTrue(complete.get(), "Timed out waiting for the preference UI");
  }
}
