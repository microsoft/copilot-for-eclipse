// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.google.gson.Gson;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelCapabilitiesSupports;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelBilling;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelBillingTokenPrices;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel.CopilotModelTokenPriceTier;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;

@ExtendWith(MockitoExtension.class)
class ModelServiceTests {

  private static final String TEST_USER = "model-service-test-user";
  private static final Gson GSON = new Gson();

  @Mock
  private CopilotLanguageServerConnection lsConnection;

  @Mock
  private AuthStatusManager authStatusManager;

  @TempDir
  private Path persistenceDirectory;

  private ModelService modelService;
  private PreferenceStorage preferenceStorage;
  private FeatureFlags featureFlags;
  private boolean previewFeaturesEnabled;

  @BeforeEach
  void setUp() {
    ChatPersistence persistence = new ChatPersistence();
    persistence.setPath(persistenceDirectory.toString());
    ByokListModelResponse byokModels = new ByokListModelResponse();
    byokModels.setModels(List.of());

    when(authStatusManager.isSignedIn()).thenReturn(true);
    when(authStatusManager.getUserName()).thenReturn(TEST_USER);
    when(lsConnection.persistence()).thenReturn(CompletableFuture.completedFuture(persistence));
    when(lsConnection.listByokModels(any())).thenReturn(CompletableFuture.completedFuture(byokModels));

    featureFlags = CopilotCore.getPlugin().getFeatureFlags();
    assertNotNull(featureFlags);
    previewFeaturesEnabled = featureFlags.isClientPreviewFeatureEnabled();
    featureFlags.setClientPreviewFeatureEnabled(false);
    preferenceStorage = new PreferenceStorage(lsConnection, authStatusManager);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (modelService != null) {
      modelService.dispose();
    }
    featureFlags.setClientPreviewFeatureEnabled(previewFeaturesEnabled);
    // JUnit removes @TempDir next; accepted account-bound writes must finish before that cleanup.
    preferenceStorage.beginShutdown().toCompletableFuture().get(5, TimeUnit.SECONDS);
    preferenceStorage.dispose();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testOptionSelection_AccountInvalidatedDuringScheduling_DoesNotPublishOrThrow(boolean context) throws Exception {
    CopilotModel model = createModel("reasoning", "Reasoning", true);
    model.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(true, List.of("low", "high"), true), null));
    when(lsConnection.listModels()).thenReturn(CompletableFuture.completedFuture(new CopilotModel[] {model}));
    ExecutorService delegate = Executors.newSingleThreadExecutor();
    ExecutorService worker = Mockito.mock(ExecutorService.class);
    AtomicBoolean invalidate = new AtomicBoolean();
    Mockito.doAnswer(invocation -> {
      if (invalidate.getAndSet(false)) {
        when(authStatusManager.isSignedIn()).thenReturn(false);
      }
      delegate.execute(invocation.getArgument(0));
      return null;
    }).when(worker).execute(any(Runnable.class));
    preferenceStorage.dispose();
    preferenceStorage = new PreferenceStorage(lsConnection, authStatusManager,
        DisplayRealm.getRealm(Display.getDefault()), worker, Executors.newSingleThreadScheduledExecutor(),
        System::nanoTime, new PreferenceFileAccess());
    try {
      modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
      waitUntil(() -> "reasoning".equals(getActiveModelId()));
      invalidate.set(true);
      CompletableFuture<Void> result = new CompletableFuture<>();
      Display.getDefault().asyncExec(() -> {
        try {
          if (context) {
            modelService.setSelectedContextWindow(model, 1000000);
          } else {
            modelService.setSelectedReasoningEffort(model, "high");
          }
          assertNull(modelService.getActiveModel());
          result.complete(null);
        } catch (Throwable failure) {
          result.completeExceptionally(failure);
        }
      });
      result.get(5, TimeUnit.SECONDS);
      assertEquals(PreferenceStorage.State.UNAVAILABLE, preferenceStorage.getState());
    } finally {
      delegate.shutdown();
    }
  }

  @Test
  void testSelection_DelayedSave_UpdatesModelAndOptionsBeforeFurtherSwtAction() throws Exception {
    CopilotModel initial = createModel("initial", "Initial", true);
    CopilotModel chosen = createModel("chosen", "Chosen", false);
    chosen.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(true, List.of("low", "high"), true), null));
    chosen.setBilling(new CopilotModelBilling(true, 1, true, new CopilotModelBillingTokenPrices(1000.0,
        new CopilotModelTokenPriceTier(1.0, 1.0, 1.0, 128000),
        new CopilotModelTokenPriceTier(2.0, 2.0, 2.0, 1000000))));
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] {initial, chosen}));
    CountDownLatch writing = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicBoolean observed = new AtomicBoolean();
    AtomicReference<ISideEffect> binding = new AtomicReference<>();
    preferenceStorage.dispose();
    preferenceStorage = new PreferenceStorage(lsConnection, authStatusManager,
        DisplayRealm.getRealm(Display.getDefault()), Executors.newSingleThreadExecutor(),
        Executors.newSingleThreadScheduledExecutor(), System::nanoTime, new PreferenceStorage.FileAccess() {
          @Override
          public String read(Path path) throws IOException {
            return Files.readString(path);
          }

          @Override
          public void write(Path path, String content) throws IOException {
            writing.countDown();
            try {
              if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Delayed save was not released");
              }
            } catch (InterruptedException exception) {
              Thread.currentThread().interrupt();
              throw new IOException(exception);
            }
            new PreferenceFileAccess().write(path, content);
          }
        });
    try {
      modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
      waitUntil(() -> "initial".equals(getActiveModelId()));
      CompletableFuture<Void> uiAction = new CompletableFuture<>();
      Display.getDefault().asyncExec(() -> {
        try {
          Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()), () -> binding.set(
              ISideEffect.create(modelService::getActiveModel, model -> observed.set(model == chosen))));
          modelService.setActiveModel("Chosen");
          modelService.setSelectedReasoningEffort(chosen, "high");
          modelService.setSelectedContextWindow(chosen, 1000000);
          assertSame(chosen, modelService.getActiveModel());
          assertEquals("high", modelService.resolveEffectiveReasoningEffort(chosen));
          assertEquals(1000000, modelService.resolveEffectiveContextWindow(chosen));
          assertEquals(chosen.getModelKey(), preferenceStorage.getReadyPreferences().getChatModel());
          assertEquals("high", preferenceStorage.getReadyPreferences().getReasoningEffort(chosen.getModelKey()));
          assertTrue(preferenceStorage.isDirty());
          Display.getDefault().asyncExec(() -> uiAction.complete(null));
        } catch (Throwable error) {
          uiAction.completeExceptionally(error);
        }
      });
      uiAction.get(5, TimeUnit.SECONDS);
      assertTrue(writing.await(5, TimeUnit.SECONDS));
      waitUntil(observed::get);
      release.countDown();
      waitUntil(() -> !preferenceStorage.isDirty());
      UserPreference restored = GSON.fromJson(Files.readString(getPreferenceFile()), UserPreference.class);
      assertEquals(chosen.getModelKey(), restored.getChatModel());
      assertEquals("high", restored.getReasoningEffort(chosen.getModelKey()));
      assertEquals(1000000, restored.getContextWindow(chosen.getModelKey()));
    } finally {
      release.countDown();
      Display.getDefault().syncExec(() -> {
        if (binding.get() != null) {
          binding.get().dispose();
        }
      });
    }
  }

  @Test
  void testInitialize_PendingPreferences_UpdatesVisionBindingWhenRestored() throws InterruptedException {
    CompletableFuture<ChatPersistence> pending = new CompletableFuture<>();
    when(lsConnection.persistence()).thenReturn(pending);
    CopilotModel visionModel = createModel("vision", "Vision", true);
    visionModel.setCapabilities(new CopilotModelCapabilities(
        new CopilotModelCapabilitiesSupports(true, List.of(), false), null));
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] {visionModel}));
    AtomicBoolean supportsVision = new AtomicBoolean();
    AtomicBoolean uiActionProcessed = new AtomicBoolean();
    AtomicReference<ISideEffect> binding = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
      Realm.runWithDefault(DisplayRealm.getRealm(Display.getDefault()),
          () -> binding.set(ISideEffect.create(modelService::isVisionSupported, supportsVision::set)));
      Display.getDefault().asyncExec(() -> uiActionProcessed.set(true));
    });
    try {
      waitUntil(uiActionProcessed::get);
      assertFalse(pending.isDone());
      assertFalse(supportsVision.get());
      assertEquals(PreferenceStorage.State.LOADING, preferenceStorage.getState());

      ChatPersistence persistence = new ChatPersistence();
      persistence.setPath(persistenceDirectory.toString());
      pending.complete(persistence);

      waitUntil(supportsVision::get);
      assertEquals(PreferenceStorage.State.READY, preferenceStorage.getState());
      assertEquals("vision", getActiveModelId());
    } finally {
      Display.getDefault().syncExec(() -> binding.get().dispose());
    }
  }

  @Test
  void testDispose_ActiveModelIsUnavailableWithoutAccessingDisposedObservable() throws Exception {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] {defaultModel}));
    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> defaultModel.getId().equals(getActiveModelId()));

    CompletableFuture<CopilotModel> result = new CompletableFuture<>();
    Display.getDefault().syncExec(() -> {
      try {
        modelService.dispose();
        result.complete(modelService.getActiveModel());
      } catch (Throwable failure) {
        result.completeExceptionally(failure);
      }
    });

    assertNull(result.get(5, TimeUnit.SECONDS));
  }

  @Test
  void testAutoModelAvailableWhenEditorPreviewDisabled() throws InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);

    waitUntil(() -> isModelAvailable(defaultModel.getModelKey()));
    assertTrue(isModelAvailable(autoModel.getModelKey()));
  }

  @Test
  void testAutoModelPolicyChangeRefreshesModelInventory() throws InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    when(lsConnection.listModels()).thenReturn(
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel }),
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> isModelAvailable(autoModel.getModelKey()));

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    assertNotNull(eventBroker);
    eventBroker.post(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, Boolean.FALSE);

    waitUntil(() -> !isModelAvailable(autoModel.getModelKey()));
  }

  @Test
  void testAutoPolicyDisableSelectsServerDefaultAndKeepsAutoPreference()
      throws IOException, InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    CopilotModel otherModel = createModel("aaa-other", "Other", false);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    writePersistedModel(autoModel.getModelKey());
    when(lsConnection.listModels()).thenReturn(
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel, otherModel }),
        CompletableFuture.completedFuture(new CopilotModel[] { otherModel, defaultModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> autoModel.getId().equals(getActiveModelId()));

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    assertNotNull(eventBroker);
    eventBroker.post(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, Boolean.FALSE);

    waitUntil(() -> defaultModel.getId().equals(getActiveModelId()));
    assertPersistedModelRemains(autoModel.getModelKey());
  }

  @Test
  void testAutoPolicyDisableUsesDeterministicFallbackAndKeepsAutoPreference()
      throws IOException, InterruptedException {
    CopilotModel firstModel = createModel("aaa-model", "First", false);
    CopilotModel lastModel = createModel("zzz-model", "Last", false);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    writePersistedModel(autoModel.getModelKey());
    when(lsConnection.listModels()).thenReturn(
        CompletableFuture.completedFuture(new CopilotModel[] { autoModel, lastModel, firstModel }),
        CompletableFuture.completedFuture(new CopilotModel[] { lastModel, firstModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> autoModel.getId().equals(getActiveModelId()));

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    assertNotNull(eventBroker);
    eventBroker.post(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, Boolean.FALSE);

    waitUntil(() -> firstModel.getId().equals(getActiveModelId()));
    assertPersistedModelRemains(autoModel.getModelKey());
  }

  @Test
  void testAutoPolicyReEnableRestoresPersistedAutoPreference() throws IOException, InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    writePersistedModel(autoModel.getModelKey());
    when(lsConnection.listModels()).thenReturn(
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel }),
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel }),
        CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> autoModel.getId().equals(getActiveModelId()));

    IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    assertNotNull(eventBroker);
    eventBroker.post(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, Boolean.FALSE);
    waitUntil(() -> defaultModel.getId().equals(getActiveModelId()));

    eventBroker.post(CopilotEventConstants.TOPIC_DID_CHANGE_AUTO_MODEL_POLICY, Boolean.TRUE);

    waitUntil(() -> autoModel.getId().equals(getActiveModelId()));
  }

  @Test
  void testDefaultKeyCollisionSelectsModelFromCurrentInventory() throws IOException, InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "Default", true);
    CopilotModel inventoryModel = createModel("gpt-4o", "Inventory", false);
    writePersistedModel("unavailable-model");
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, inventoryModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> isModelAvailable(defaultModel.getModelKey()));

    AtomicReference<CopilotModel> activeModel = new AtomicReference<>();
    AtomicReference<CopilotModel> pickerModel = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      activeModel.set(modelService.getActiveModel());
      pickerModel.set(modelService.getModels().get(defaultModel.getModelKey()));
    });

    assertSame(inventoryModel, pickerModel.get());
    assertSame(pickerModel.get(), activeModel.get());
  }

  @Test
  void testSetActiveModel_AlreadyActiveModelDoesNotPersistPreference() throws InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] { defaultModel }));

    modelService = new ModelService(lsConnection, authStatusManager, preferenceStorage);
    waitUntil(() -> defaultModel.getId().equals(getActiveModelId()));

    AtomicReference<CopilotModel> activeModel = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      modelService.setActiveModel(defaultModel.getModelName());
      activeModel.set(modelService.getActiveModel());
    });

    assertSame(defaultModel, activeModel.get());
    Thread.sleep(100);
    assertFalse(Files.exists(getPreferenceFile()));
  }

  private static CopilotModel createModel(String id, String name, boolean isChatDefault) {
    CopilotModel model = new CopilotModel();
    model.setId(id);
    model.setModelName(name);
    model.setModelFamily(id);
    model.setScopes(List.of(CopilotScope.CHAT_PANEL, CopilotScope.AGENT_PANEL));
    model.setChatDefault(isChatDefault);
    return model;
  }

  private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
      Thread.sleep(25);
    }
    assertTrue(condition.getAsBoolean(), "Timed out waiting for model service state update");
  }

  private boolean isModelAvailable(String modelKey) {
    AtomicBoolean available = new AtomicBoolean();
    Display.getDefault().syncExec(() -> available.set(modelService.getModels().containsKey(modelKey)));
    return available.get();
  }

  private String getActiveModelId() {
    AtomicReference<String> activeModelId = new AtomicReference<>();
    Display.getDefault().syncExec(() -> {
      CopilotModel activeModel = modelService.getActiveModel();
      activeModelId.set(activeModel == null ? null : activeModel.getId());
    });
    return activeModelId.get();
  }

  private void writePersistedModel(String modelKey) throws IOException {
    UserPreference preference = new UserPreference();
    preference.setChatModel(modelKey);
    Path preferenceFile = getPreferenceFile();
    Files.createDirectories(preferenceFile.getParent());
    Files.writeString(preferenceFile, GSON.toJson(preference));
  }

  private void assertPersistedModelRemains(String expectedModelKey) throws IOException, InterruptedException {
    long deadline = System.currentTimeMillis() + 500;
    while (System.currentTimeMillis() < deadline) {
      assertEquals(expectedModelKey, readPersistedModel());
      Thread.sleep(25);
    }
  }

  private String readPersistedModel() throws IOException {
    Path preferenceFile = getPreferenceFile();
    if (!Files.exists(preferenceFile)) {
      return null;
    }
    UserPreference preference = GSON.fromJson(Files.readString(preferenceFile), UserPreference.class);
    return preference == null ? null : preference.getChatModel();
  }

  private Path getPreferenceFile() {
    return persistenceDirectory.resolve(TEST_USER).resolve("pref.json");
  }
}
