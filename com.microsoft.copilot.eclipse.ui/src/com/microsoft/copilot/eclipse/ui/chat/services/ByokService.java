// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokApiKey;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModelProvider;
import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;

/**
 * Service for managing BYOK (Bring Your Own Key) functionality. Handles API keys, models, and provider-specific
 * operations.
 */
public class ByokService extends ChatBaseService {

  // Observable data for UI binding
  private IObservableValue<Map<String, List<ByokModel>>> byokModelsByProviderObservable;
  private IObservableValue<Map<String, String>> apiKeysObservable;
  // Feature flag observable (byokEnabled)
  private IObservableValue<Boolean> byokEnabledObservable;

  // Event broker for publishing model updates
  private IEventBroker eventBroker;
  private EventHandler featureFlagEventHandler;

  // UI binding
  private ISideEffect modelsSideEffect;
  private ISideEffect apiKeysSideEffect;
  private ISideEffect byokFlagSideEffect;

  /**
   * Constructor.
   */
  public ByokService(CopilotLanguageServerConnection lsConnection) {
    super(lsConnection, null);

    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

    ensureRealm(() -> {
      byokModelsByProviderObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      apiKeysObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      byokEnabledObservable = new WritableValue<>(
          CopilotCore.getPlugin().getFeatureFlags().isByokEnabled(), Boolean.class);
    });

    // Subscribe to feature flag changes for BYOK
    if (eventBroker != null) {
      featureFlagEventHandler = event -> {
        Object params = event.getProperty(IEventBroker.DATA);
        if (params instanceof DidChangeFeatureFlagsParams featureFlagsParams) {
          ensureRealm(() -> byokEnabledObservable.setValue(featureFlagsParams.isByokEnabled()));
        }
      };
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS, featureFlagEventHandler);
    }
  }

  /**
   * Bind a ByokPreferencePage to this service for automatic updates.
   */
  public void bindByokPreferencePage(ByokPreferencePage page) {
    ensureRealm(() -> {
      unbindByokPreferencePage();

      // Create side effect for model updates
      modelsSideEffect = ISideEffect.create(() -> {
        return byokModelsByProviderObservable.getValue();
      }, page::updateModelsDisplay);

      // Create side effect for API keys updates
      apiKeysSideEffect = ISideEffect.create(() -> {
        return apiKeysObservable.getValue();
      }, page::updateApiKeysDisplay);

      // Create side effect for byok flag updates
      byokFlagSideEffect = ISideEffect.create(() -> byokEnabledObservable.getValue(),
          flagValue -> page.updatePageState());
    });
  }

  /**
   * dispose the binding to ByokPreferencePage.
   */
  public void unbindByokPreferencePage() {
    if (modelsSideEffect != null) {
      modelsSideEffect.dispose();
      modelsSideEffect = null;
    }

    if (apiKeysSideEffect != null) {
      apiKeysSideEffect.dispose();
      apiKeysSideEffect = null;
    }

    if (byokFlagSideEffect != null) {
      byokFlagSideEffect.dispose();
      byokFlagSideEffect = null;
    }
  }

  /**
   * Load API keys from persistent storage.
   */
  public CompletableFuture<Void> loadApiKeys() {
    return lsConnection.listByokApiKeys(new ByokApiKey(null, null)).thenAccept(response -> {
      if (response != null && response.getApiKeys() != null) {
        Map<String, String> apiKeysByProvider = response.getApiKeys().stream()
            .collect(Collectors.toMap(ByokApiKey::getProviderName, ByokApiKey::getApiKey, (k1, k2) -> k1));
        ensureRealm(() -> {
          apiKeysObservable.setValue(apiKeysByProvider);
        });
      }
    });
  }

  /**
   * Load BYOK models from persistent storage.
   */
  public CompletableFuture<Void> loadLocalModels() {
    return lsConnection.listByokModels(new ByokListModelParams(null, false)).thenAccept(response -> {
      if (response != null && response.getModels() != null) {
        Map<String, List<ByokModel>> modelsByProvider = response.getModels().stream()
            .collect(Collectors.groupingBy(ByokModel::getProviderName));
        ensureRealm(() -> {
          byokModelsByProviderObservable.setValue(modelsByProvider);
          notifyModelUpdate(modelsByProvider);
        });
      }
    });
  }

  /**
   * Refresh BYOK data (including API keys and models).
   */
  public CompletableFuture<Void> refreshData() {
    return loadApiKeys().thenCompose(unused -> loadLocalModels());
  }

  /**
   * Save a BYOK model. Sequence: saveModel() -> loadLocalModels(PROVIDER).
   */
  public CompletableFuture<Void> saveModel(ByokModel model) {
    return lsConnection.saveByokModel(model).thenCompose(response -> {
      if (!response.isSuccess()) {
        String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to save model";
        return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
      }
      return loadLocalModels();
    });
  }

  /**
   * Delete a BYOK model. Sequence: deleteModel() -> loadLocalModels(PROVIDER).
   */
  public CompletableFuture<Void> deleteModel(ByokModel model) {
    return lsConnection.deleteByokModel(model).thenCompose(response -> {
      if (!response.isSuccess()) {
        String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to delete model";
        return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
      }
      return loadLocalModels();
    });
  }

  /**
   * Add API key and register models. Flow: saveApiKey -> listRemoteModels(provider, true) -> batchSave(models
   * registered) -> refreshData(PROVIDER). rollbackOnListFailure=true means if list fails (e.g. invalid key) we delete
   * the just-saved key.
   */
  public CompletableFuture<Void> addApiKey(String providerName, String apiKey) {
    ByokApiKey key = new ByokApiKey(providerName, null);
    key.setApiKey(apiKey);

    return lsConnection.saveByokApiKey(key).thenCompose(saveResp -> {
      if (!saveResp.isSuccess()) {
        String errorMessage = saveResp.getMessage() != null ? saveResp.getMessage() : "Failed to save API key";
        return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
      }
      return lsConnection.listByokModels(new ByokListModelParams(providerName, true)).exceptionally(ex -> {
        lsConnection.deleteByokApiKey(new ByokApiKey(providerName, null));
        throw new IllegalStateException("Invalid API key", ex);
      }).thenCompose(listResp -> {
        if (listResp == null || listResp.getModels() == null || listResp.getModels().isEmpty()) {
          return refreshData();
        }
        // Special Case: OpenRouter has too many default models(about 180) so skip registration.
        List<ByokModel> fetched = listResp.getModels();
        boolean needRegister = !ByokModelProvider.OPENROUTER.getDisplayName().equals(providerName);
        fetched.forEach(m -> m.setRegistered(needRegister));
        return batchSaveByokModels(fetched).thenCompose(v -> refreshData());
      });
    });
  }

  /**
   * Change an existing API key. Flow: saveApiKey -> listRemoteModels(provider, true) -> mergeRemoteWithLocal()
   * ->refreshData(PROVIDER)
   */
  public CompletableFuture<Void> changeApiKey(String providerName, String newApiKey) {
    ByokApiKey key = new ByokApiKey(providerName, null);
    key.setApiKey(newApiKey);
    return lsConnection.saveByokApiKey(key).thenCompose(saveResp -> {
      if (!saveResp.isSuccess()) {
        String errorMessage = saveResp.getMessage() != null ? saveResp.getMessage() : "Failed to save API key";
        return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
      }
      return lsConnection.listByokModels(new ByokListModelParams(providerName, true));
    }).thenCompose(listResp -> {
      if (listResp == null || listResp.getModels() == null || listResp.getModels().isEmpty()) {
        return refreshData();
      }
      // Only add newly discovered models (unregistered). Existing model states preserved.
      return mergeRemoteModelsWithLocal(providerName, listResp.getModels()).thenCompose(changed -> refreshData());
    });
  }

  /**
   * Delete API key for a provider. Sequence: deleteApiKey() -> refreshData(PROVIDER)
   */
  public CompletableFuture<Void> deleteApiKey(String providerName) {
    ByokApiKey byokApiKey = new ByokApiKey(providerName, null);

    return lsConnection.deleteByokApiKey(byokApiKey).thenCompose(response -> {
      if (!response.isSuccess()) {
        String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to delete API key";
        return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
      }
      return refreshData();
    });
  }

  /**
   * Reload a single provider's complete data (API keys, local models, and remote models if applicable).
   */
  public CompletableFuture<Void> reloadProvider(String providerName) {
    if (ByokModelProvider.isAzure(providerName)) {
      return loadLocalModels();
    }

    final AtomicBoolean hasApiKey = new AtomicBoolean(false);
    ensureRealm(() -> {
      Map<String, String> currentKeys = apiKeysObservable != null ? apiKeysObservable.getValue() : null;
      if (currentKeys != null && currentKeys.containsKey(providerName)) {
        hasApiKey.set(true);
      }
    });
    // if there is no API key for this non-Azure provider, skip remote fetch.
    if (!hasApiKey.get()) {
      return CompletableFuture.completedFuture(null);
    }

    return fetchProviderModels(providerName).thenCompose(changed -> loadLocalModels());
  }

  /**
   * Reload all providers sequentially to avoid file write conflicts. Only providers with API keys (excluding Azure)
   * will fetch remote models.
   */
  public CompletableFuture<Void> reloadAllProviders() {
    return fetchAllProvidersSequentially()
        .thenCompose(changed -> changed ? refreshData() : CompletableFuture.completedFuture(null));
  }

  /**
   * Batch save BYOK models with CompletableFuture. Models are saved sequentially to avoid file write conflicts.
   */
  private CompletableFuture<Void> batchSaveByokModels(List<ByokModel> models) {
    if (models == null || models.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    List<ByokModel> validModels = models.stream().filter(m -> m != null).toList();
    if (validModels.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

    // need to save models sequentially to avoid file write conflicts from cls
    for (ByokModel model : validModels) {
      result = result.thenCompose(unused -> lsConnection.saveByokModel(model).thenAccept(response -> {
        if (!response.isSuccess()) {
          String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to save BYOK model";
          throw new IllegalStateException(errorMessage);
        }
      }));
    }

    return result;
  }

  /**
   * Merge remote models with local models, adding newly discovered remote models (unregistered) to storage. Returns
   * true if new models were persisted.
   */
  private CompletableFuture<Boolean> mergeRemoteModelsWithLocal(String providerName, List<ByokModel> remoteModels) {
    if (remoteModels == null || remoteModels.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    AtomicReference<Set<String>> localIdsRef = new AtomicReference<>(Collections.emptySet());
    ensureRealm(() -> {
      Map<String, List<ByokModel>> modelsFromAllProviders = byokModelsByProviderObservable.getValue();
      List<ByokModel> localModels = modelsFromAllProviders.getOrDefault(providerName, List.of());
      localIdsRef.set(localModels.stream().map(ByokModel::getModelId).collect(Collectors.toSet()));
    });
    Set<String> localIds = localIdsRef.get();
    List<ByokModel> toAdd = new ArrayList<>();
    for (ByokModel remoteModel : remoteModels) {
      if (!localIds.contains(remoteModel.getModelId())) {
        remoteModel.setRegistered(false); // newly discovered, keep as unregistered
        toAdd.add(remoteModel);
      }
    }
    if (toAdd.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    return batchSaveByokModels(toAdd).thenApply(v -> true).exceptionally(ex -> {
      CopilotCore.LOGGER.error("Failed to persist newly fetched remote models for provider: " + providerName, ex);
      return false; // treat as no applied change
    });
  }

  /**
   * Fetch remote models (remote=true) for a specific provider and merge new ones into local storage. Returns true if
   * new models were added (and saved), false otherwise.
   */
  public CompletableFuture<Boolean> fetchProviderModels(String providerName) {
    return lsConnection.listByokModels(new ByokListModelParams(providerName, true)).thenCompose(response -> {
      if (response == null || response.getModels() == null || response.getModels().isEmpty()) {
        return CompletableFuture.completedFuture(false);
      }
      return mergeRemoteModelsWithLocal(providerName, response.getModels());
    });
  }

  /**
   * Fetch remote models for all providers sequentially.
   */
  private CompletableFuture<Boolean> fetchAllProvidersSequentially() {
    AtomicReference<List<String>> providersRef = new AtomicReference<>(List.of());
    ensureRealm(() -> {
      Map<String, String> currentApiKeys = apiKeysObservable.getValue();
      if (currentApiKeys == null || currentApiKeys.isEmpty()) {
        providersRef.set(List.of());
        return;
      }
      List<String> providersToFetch = currentApiKeys.keySet().stream()
          .filter(providerName -> !ByokModelProvider.isAzure(providerName)).toList();
      providersRef.set(providersToFetch);
    });
    List<String> providersToFetch = providersRef.get();
    if (providersToFetch.isEmpty()) {
      return CompletableFuture.completedFuture(false);
    }
    AtomicBoolean anyChanged = new AtomicBoolean(false);
    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    for (String providerName : providersToFetch) {
      chain = chain.thenCompose(unused -> fetchProviderModels(providerName).thenAccept(changed -> {
        if (Boolean.TRUE.equals(changed)) {
          anyChanged.set(true);
        }
      }));
    }
    return chain.thenApply(unused -> anyChanged.get());
  }

  private void notifyModelUpdate(Map<String, List<ByokModel>> models) {
    if (eventBroker != null) {
      eventBroker.post(CopilotEventConstants.TOPIC_CHAT_BYOK_MODELS_UPDATED, models);
    }
  }

  /**
   * Dispose service and clean up resources.
   */
  public void dispose() {
    unbindByokPreferencePage();
    if (eventBroker != null && featureFlagEventHandler != null) {
      eventBroker.unsubscribe(featureFlagEventHandler);
      featureFlagEventHandler = null;
    }
  }
}
