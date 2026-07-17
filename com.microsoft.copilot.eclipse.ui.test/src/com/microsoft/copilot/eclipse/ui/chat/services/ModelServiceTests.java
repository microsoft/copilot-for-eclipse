// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.google.gson.Gson;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
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
  private FeatureFlags featureFlags;
  private boolean previewFeaturesEnabled;

  @BeforeEach
  void setUp() {
    new PreferenceCacheResetter(lsConnection, authStatusManager).reset();

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
  }

  @AfterEach
  void tearDown() {
    if (modelService != null) {
      modelService.dispose();
    }
    featureFlags.setClientPreviewFeatureEnabled(previewFeaturesEnabled);
    new PreferenceCacheResetter(lsConnection, authStatusManager).reset();
  }

  @Test
  void testAutoModelAvailableWhenEditorPreviewDisabled() throws InterruptedException {
    CopilotModel defaultModel = createModel("gpt-4o", "GPT-4o", true);
    CopilotModel autoModel = createModel("auto", "Auto", false);
    when(lsConnection.listModels())
        .thenReturn(CompletableFuture.completedFuture(new CopilotModel[] { defaultModel, autoModel }));

    modelService = new ModelService(lsConnection, authStatusManager);

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

    modelService = new ModelService(lsConnection, authStatusManager);
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

    modelService = new ModelService(lsConnection, authStatusManager);
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

    modelService = new ModelService(lsConnection, authStatusManager);
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

    modelService = new ModelService(lsConnection, authStatusManager);
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

    modelService = new ModelService(lsConnection, authStatusManager);
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
    return persistenceDirectory.resolve(TEST_USER).resolve(ChatBaseService.PREF_FILE_NAME);
  }

  private static final class PreferenceCacheResetter extends ChatBaseService {

    private PreferenceCacheResetter(CopilotLanguageServerConnection lsConnection,
        AuthStatusManager authStatusManager) {
      super(lsConnection, authStatusManager);
    }

    private void reset() {
      clearUserPreferenceCache();
    }
  }
}
