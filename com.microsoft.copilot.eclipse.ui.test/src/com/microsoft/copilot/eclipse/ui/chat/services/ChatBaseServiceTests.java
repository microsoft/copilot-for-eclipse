// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.ui.swt.DropdownButton;

@ExtendWith(MockitoExtension.class)
class ChatBaseServiceTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private AuthStatusManager mockAuthStatusManager;

  @TempDir
  private Path directory;

  @Test
  void persistUserPreference_WhenNotSignedIn_ShouldSkipLspInteraction() {
    when(mockAuthStatusManager.isSignedIn()).thenReturn(false);
    PreferenceStorage storage = new PreferenceStorage(mockLsConnection, mockAuthStatusManager);
    try {
      storage.initialize();
      storage.persist();
      assertEquals(PreferenceStorage.State.UNAVAILABLE, storage.getState());
      verifyNoInteractions(mockLsConnection);
    } finally {
      storage.dispose();
    }
  }

  @Test
  void testFirstInitialization_PendingAuthenticatedPersistence_DoesNotBlockSwt() throws Exception {
    when(mockAuthStatusManager.isSignedIn()).thenReturn(true);
    when(mockAuthStatusManager.getUserName()).thenReturn("pending-user");
    when(mockAuthStatusManager.getQuotaStatus()).thenReturn(CheckQuotaResult.empty());
    CompletableFuture<ChatPersistence> persistence = new CompletableFuture<>();
    when(mockLsConnection.persistence()).thenReturn(persistence);
    CopilotModel model = new CopilotModel();
    model.setId("saved-model");
    model.setModelName("Saved model");
    model.setModelFamily("family");
    model.setScopes(List.of(CopilotScope.CHAT_PANEL, CopilotScope.AGENT_PANEL));
    when(mockLsConnection.listModels()).thenReturn(CompletableFuture.completedFuture(new CopilotModel[] {model}));
    Path file = directory.resolve("pending-user").resolve("pref.json");
    Files.createDirectories(file.getParent());
    String saved = "{\"chatModeName\":\"Ask\",\"chatModel\":\"" + model.getModelKey() + "\"}";
    Files.writeString(file, saved);
    ByokListModelResponse byok = new ByokListModelResponse();
    byok.setModels(List.of());
    when(mockLsConnection.listByokModels(org.mockito.ArgumentMatchers.any()))
        .thenReturn(CompletableFuture.completedFuture(byok));

    AtomicReference<PreferenceStorage> storage = new AtomicReference<>();
    AtomicReference<UserPreferenceService> preferences = new AtomicReference<>();
    AtomicReference<ModelService> models = new AtomicReference<>();
    AtomicReference<Shell> shell = new AtomicReference<>();
    AtomicReference<DropdownButton> modeControl = new AtomicReference<>();
    AtomicReference<DropdownButton> modelControl = new AtomicReference<>();
    CompletableFuture<Boolean> uiAction = new CompletableFuture<>();
    Display.getDefault().asyncExec(() -> {
      try {
        storage.set(new PreferenceStorage(mockLsConnection, mockAuthStatusManager));
        models.set(new ModelService(mockLsConnection, mockAuthStatusManager, storage.get()));
        preferences.set(new UserPreferenceService(mockLsConnection, mockAuthStatusManager, storage.get()));
        shell.set(new Shell(Display.getDefault()));
        DropdownButton modePicker = new DropdownButton(shell.get(), SWT.NONE);
        DropdownButton modelPicker = new DropdownButton(shell.get(), SWT.NONE);
        modeControl.set(modePicker);
        modelControl.set(modelPicker);
        preferences.get().bindChatModePicker(modePicker);
        models.get().bindModelPicker(modelPicker);
        Display.getDefault().asyncExec(() -> {
          uiAction.complete(!modePicker.getEnabled() && !modelPicker.getEnabled()
              && storage.get().getState() == PreferenceStorage.State.LOADING);
        });
      } catch (Throwable error) {
        uiAction.completeExceptionally(error);
      }
    });
    try {
      assertTrue(uiAction.get(5, TimeUnit.SECONDS), "Preference controls must stay disabled while SWT processes work");
      assertFalse(persistence.isDone(), "SWT must process another action before persistence completes");
      Display.getDefault().syncExec(() -> assertNull(models.get().getActiveModel()));
      ChatPersistence response = new ChatPersistence();
      response.setPath(directory.toString());
      persistence.complete(response);
      AtomicReference<Boolean> restored = new AtomicReference<>(false);
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      do {
        Display.getDefault().syncExec(() -> restored.set(
            "Ask".equals(preferences.get().getActiveModeNameOrId())
                && models.get().getActiveModel() == model
                && modeControl.get().getEnabled() && modelControl.get().getEnabled()));
        if (restored.get()) {
          break;
        }
        Thread.sleep(10);
      } while (System.nanoTime() < deadline);
      assertTrue(restored.get(), "Mode and model must restore after the shared load succeeds");
      assertEquals(saved, Files.readString(file), "Placeholder observables must not save over stored preferences");
    } finally {
      // Release a regressed blocking constructor before cleaning up on SWT.
      persistence.completeExceptionally(new IllegalStateException("test cleanup"));
      Display.getDefault().syncExec(() -> {
        if (shell.get() != null) {
          shell.get().dispose();
        }
        if (preferences.get() != null) {
          preferences.get().dispose();
        }
        if (models.get() != null) {
          models.get().dispose();
        }
        if (storage.get() != null) {
          storage.get().dispose();
        }
      });
    }
  }
}