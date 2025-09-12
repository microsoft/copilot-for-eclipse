package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
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

  // Event broker for publishing model updates
  private IEventBroker eventBroker;

  // UI binding
  private ISideEffect modelsSideEffect;

  /**
   * Constructor.
   */
  public ByokService(CopilotLanguageServerConnection lsConnection) {
    super(lsConnection, null);

    this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);

    ensureRealm(() -> {
      byokModelsByProviderObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
    });
  }
  

  /**
   * Bind a ByokPreferencePage to this service for automatic updates.
   */
  public void bindByokPreferencePage(ByokPreferencePage page) {
    ensureRealm(() -> {
      unbindByokPreferencePage();

      // Create side effect for model updates
      modelsSideEffect = ISideEffect.create(() -> {
        Map<String, List<ByokModel>> currentModels = byokModelsByProviderObservable.getValue();
        if (currentModels != null) {
          return currentModels.values().stream().flatMap(List::stream).collect(Collectors.toList());
        }
        return List.of();
      }, page::updateModelsDisplay);
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
          notifyModelUpdate();
        });
      }
    });
  }

  /**
   * Save a BYOK model.
   */
  public CompletableFuture<Void> saveModel(ByokModel model) {
    return lsConnection.saveByokModel(model).thenCompose(response -> {
      if (!response.isSuccess()) {
        String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to save model";
        return CompletableFuture.failedFuture(new RuntimeException(errorMessage));
      }
      return loadLocalModels();
    });
  }

  /**
   * Delete a BYOK model.
   */
  public CompletableFuture<Void> deleteModel(ByokModel model) {
    return lsConnection.deleteByokModel(model).thenCompose(response -> {
      if (!response.isSuccess()) {
        String errorMessage = response.getMessage() != null ? response.getMessage() : "Failed to delete model";
        return CompletableFuture.failedFuture(new RuntimeException(errorMessage));
      }
      return loadLocalModels();
    });
  }
  

  /**
   * Reload a single provider's complete data (API keys, local models, and remote models if applicable).
   */
  public CompletableFuture<Void> reloadProvider(String providerName) {
    return loadLocalModels().thenCompose(unused -> {
      // Azure doesn't need fetchProviderModels
      if (ByokModelProvider.isAzure(providerName)) {
        return CompletableFuture.completedFuture(null);
      }
      //TODO: add other providers
      return CompletableFuture.completedFuture(null);
    });
  }

  /**
   * Reload all providers sequentially to avoid file write conflicts. Only providers with API keys (excluding Azure)
   * will fetch remote models.
   */
  public CompletableFuture<Void> reloadAllProviders() {
    //for now, only need to reload azure provider
    return reloadProvider(ByokModelProvider.AZURE.getDisplayName());
  }

  private void notifyModelUpdate() {
    ensureRealm(() -> {
      Map<String, List<ByokModel>> currentModels = byokModelsByProviderObservable.getValue();

      // Publish event for model updates
      if (eventBroker != null) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_BYOK_MODELS_UPDATED, currentModels);
      }
    });
  }

  /**
   * Dispose service and clean up resources.
   */
  public void dispose() {
    unbindByokPreferencePage();
  }
}
