// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.ModelPickerGroupsBuilder;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.DropdownButton;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;

/**
 * Service for managing AI models and their selection. Handles all model-related functionality including persistence,
 * BYOK integration, UI binding, and communicates with other services through pure events.
 */
public class ModelService extends ChatBaseService {

  private static final String AUTO_MODEL_ID = "auto";

  // models for the model picker
  private IObservableValue<Map<String, CopilotModel>> modelObservable;
  private IObservableValue<CopilotModel> activeModelObservable;
  private IObservableValue<Map<String, String>> reasoningEffortObservable;
  // Used to update modelObservable
  private Map<String, CopilotModel> copilotModels = new HashMap<>();
  private Map<String, CopilotModel> registeredByokModels = new HashMap<>();
  private CopilotModel defaultModel;
  private CopilotModel fallbackModel;

  private ChatMode currentChatMode = ChatMode.Agent;

  private final Map<DropdownButton, ISideEffect[]> modelButtonSideEffects = new HashMap<>();
  private ISideEffect actionBarSideEffect;

  // Event communication
  private IEventBroker eventBroker;
  private EventHandler authStatusChangedEventHandler;
  private EventHandler chatModeChangedEventHandler;
  private EventHandler byokModelsUpdatedEventHandler;
  private EventHandler featureFlagsChangedEventHandler;
  private EventHandler customModeModelChangedEventHandler;

  /**
   * Constructor for the ModelService.
   */
  public ModelService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    super(lsConnection, authStatusManager);

    ensureRealm(() -> {
      modelObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      activeModelObservable = new WritableValue<>(null, CopilotModel.class);
      Map<String, String> initialEfforts = Map.of();
      UserPreference initialPreference = getUserPreference();
      if (initialPreference != null) {
        initialEfforts = initialPreference.getReasoningEffortSnapshot();
      }
      reasoningEffortObservable = new WritableValue<>(initialEfforts, Map.class);
    });

    initializeEventHandlers();
    subscribeToEvents();
    initializeModels();
  }

  private void initializeEventHandlers() {
    authStatusChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof CopilotStatusResult statusResult) {
        onDidCopilotStatusChange(statusResult);
      }
    };

    chatModeChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof ChatMode chatMode) {
        currentChatMode = chatMode;
        updateModelsForChatMode(chatMode);
      }
    };

    byokModelsUpdatedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof Map<?, ?> modelsMap) {
        @SuppressWarnings("unchecked")
        Map<String, List<ByokModel>> byokModels = (Map<String, List<ByokModel>>) modelsMap;
        saveRegisteredByokModels(byokModels);
        reconcileReasoningEfforts();
        ensureRealm(() -> updateModelsForChatMode(currentChatMode));
      }
    };

    // TODO: need to remove this logic after group policy is available
    featureFlagsChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof DidChangeFeatureFlagsParams) {
        initializeModels();
      }
    };

    customModeModelChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof String modelNameWithFamily) {
        // Parse the model name from format "<modelName> (<modelFamily>)"
        String modelName = modelNameWithFamily;
        int openParenIndex = modelNameWithFamily.indexOf(" (");
        if (openParenIndex > 0) {
          modelName = modelNameWithFamily.substring(0, openParenIndex).trim();
        }

        // First update chat mode to Agent to load Agent mode models
        currentChatMode = ChatMode.Agent;
        updateModelsForChatMode(ChatMode.Agent);

        // Then switch to the specified model (setActiveModel will be called after models are loaded)
        setActiveModel(modelName);
      }
    };
  }

  private void subscribeToEvents() {
    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, authStatusChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, chatModeChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_BYOK_MODELS_UPDATED, byokModelsUpdatedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS, featureFlagsChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_CUSTOM_MODE_MODEL_CHANGED,
          customModeModelChangedEventHandler);
    } else {
      CopilotCore.LOGGER.error(new IllegalStateException("Event broker is null"));
    }
  }

  private void initializeModels() {
    if (authStatusManager.isSignedIn()) {
      Job job = new Job("Fetching all models...") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            fetchCopilotModels();
            fetchByokModels();
            reconcileReasoningEfforts();
            ensureRealm(() -> {
              updateModelsForChatMode(currentChatMode);
            });

          } catch (InterruptedException | ExecutionException e) {
            CopilotCore.LOGGER.error("Failed to initialize models", e);
          }
          return Status.OK_STATUS;
        }
      };
      job.setSystem(true);
      job.schedule();
    }
  }

  private void fetchCopilotModels() throws InterruptedException, ExecutionException {
    CopilotModel[] modelArray = lsConnection.listModels().get();
    Map<String, CopilotModel> newModels = new HashMap<>();

    for (CopilotModel model : modelArray) {
      // TODO: remove it once CLS supports filtering models by preview flag
      if (!CopilotCore.getPlugin().getFeatureFlags().isClientPreviewFeatureEnabled()
          && AUTO_MODEL_ID.equals(model.getId())) {
        continue;
      }
      boolean supportsChat = model.getScopes().contains(CopilotScope.CHAT_PANEL);
      boolean supportsAgent = model.getScopes().contains(CopilotScope.AGENT_PANEL);
      if (supportsChat || supportsAgent) {
        newModels.put(model.getModelKey(), model);
      }
      if (model.isChatDefault()) {
        defaultModel = model;
      }
      if (model.isChatFallback()) {
        fallbackModel = model;
      }
    }

    copilotModels = newModels;
  }

  private void fetchByokModels() throws InterruptedException, ExecutionException {
    ByokListModelResponse response = lsConnection.listByokModels(new ByokListModelParams(null, false)).get();
    if (response != null && response.getModels() != null) {
      Map<String, List<ByokModel>> modelsByProvider = response.getModels().stream()
          .collect(java.util.stream.Collectors.groupingBy(ByokModel::getProviderName));
      saveRegisteredByokModels(modelsByProvider);
    }
  }

  private void saveRegisteredByokModels(Map<String, List<ByokModel>> byokModels) {
    Map<String, CopilotModel> newByokModels = new HashMap<>();
    for (List<ByokModel> providerModels : byokModels.values()) {
      for (ByokModel model : providerModels) {
        if (model.isRegistered()) {
          CopilotModel converted = ModelUtils.convertByokModelToCopilotModel(model);
          newByokModels.put(model.getModelKey(), converted);
        }
      }
    }
    registeredByokModels = newByokModels;
  }

  private String restoreActiveModel() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getChatModel() != null) {
      return preference.getChatModel();
    }

    if (defaultModel != null) {
      return defaultModel.getId();
    }

    return null;
  }

  private void updateModelsForChatMode(ChatMode chatMode) {
    String scope = modeToScope(chatMode);

    // Filter models for the current mode from combined models
    final Map<String, CopilotModel> modelsForCurrentMode = new HashMap<>();
    Map<String, CopilotModel> allModels = new HashMap<>();
    allModels.putAll(copilotModels);
    // TODO: need to remove this logic after group policy is available
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags == null || flags.isByokEnabled()) {
      allModels.putAll(registeredByokModels);
    }

    for (Map.Entry<String, CopilotModel> entry : allModels.entrySet()) {
      CopilotModel model = entry.getValue();
      if (model.getScopes().contains(scope)) {
        modelsForCurrentMode.put(entry.getKey(), model);
      }
    }

    ensureRealm(() -> {
      modelObservable.setValue(modelsForCurrentMode);
      // Validate and set active model for the current mode
      validateAndSetActiveModelForMode(modelsForCurrentMode, scope);
    });
  }

  /**
   * Validate and set the appropriate active model for the current chat mode. This method handles the logic of restoring
   * user preference or falling back to default.
   */
  private void validateAndSetActiveModelForMode(Map<String, CopilotModel> modelsForCurrentMode, String scope) {
    CopilotModel currentActive = getActiveModel();
    boolean isCurrentModelAvailable = currentActive != null
        && modelsForCurrentMode.containsKey(currentActive.getModelKey());
    if (currentActive == null || !isCurrentModelAvailable) {
      // Try to restore user's preferred model if it's available in current mode
      String restoredModelId = restoreActiveModel();
      if (restoredModelId != null && modelsForCurrentMode.containsKey(restoredModelId)) {
        ensureRealm(() -> activeModelObservable.setValue(modelsForCurrentMode.get(restoredModelId)));
        return;
      }
      // fall back the first available model in the current mode
      if (!modelsForCurrentMode.isEmpty()) {
        CopilotModel firstModel = modelsForCurrentMode.values().iterator().next();
        ensureRealm(() -> activeModelObservable.setValue(firstModel));
      }
    }
  }

  private String modeToScope(ChatMode mode) {
    if (mode == null) {
      return "";
    }

    switch (mode) {
      case Ask:
        return CopilotScope.CHAT_PANEL;
      case Agent:
        return CopilotScope.AGENT_PANEL;
      default:
        return "";
    }
  }

  private void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    String status = copilotStatusResult.getStatus();
    switch (status) {
      case CopilotStatusResult.OK, CopilotStatusResult.NOT_AUTHORIZED:
        initializeModels();
        break;
      default:
        disposeAllSideEffects();
        break;
    }
  }

  /**
   * Set the active model by name.
   *
   * @param modelName the name of the model
   */
  public void setActiveModel(String modelName) {
    Map<String, CopilotModel> currentModels = modelObservable.getValue();

    // Find model by model name and get its composite key
    String compositeKey = null;
    final CopilotModel model;
    CopilotModel foundModel = null;

    for (Map.Entry<String, CopilotModel> entry : currentModels.entrySet()) {
      if (entry.getValue().getModelName().equals(modelName)) {
        compositeKey = entry.getKey();
        foundModel = entry.getValue();
        break;
      }
    }
    model = foundModel;
    if (model != null && compositeKey != null) {
      // Persist using the composite key for proper identification
      UserPreference preference = getUserPreference();
      preference.setChatModel(compositeKey);
      // Persist asynchronously to avoid deadlock: persistUserPreference() calls
      // persistence().get() which blocks waiting for the LSP listener thread.
      // If called on the UI thread while the listener is in syncExec, both threads
      // deadlock.
      CompletableFuture.runAsync(this::persistUserPreference);

      // Update observable
      ensureRealm(() -> activeModelObservable.setValue(model));
    }
  }

  /**
   * Get the active model.
   *
   * @return the active model
   */
  public CopilotModel getActiveModel() {
    return activeModelObservable.getValue();
  }

  /**
   * Get all available models for the current chat mode.
   *
   * @return map of model ID to CopilotModel
   */
  public Map<String, CopilotModel> getModels() {
    return modelObservable.getValue();
  }

  /**
   * Get the fallback model.
   *
   * @return the fallback model
   */
  public CopilotModel getFallbackModel() {
    return fallbackModel;
  }

  /**
   * Set the fallback model as the active model.
   */
  public void setFallBackModelAsActiveModel() {
    if (fallbackModel != null) {
      setActiveModel(fallbackModel.getModelName());
    }
  }

  /**
   * Check if the active model supports vision capabilities.
   */
  public boolean isVisionSupported() {
    CopilotModel model = getActiveModel();
    return model != null && model.getCapabilities().supports().vision();
  }

  /**
   * Returns the user-selected reasoning effort for the given model, or {@code null} when the user has not made a
   * selection. The persisted snapshot is kept in sync with the current model inventory by
   * {@link #reconcileReasoningEfforts()} after each model fetch, so this method is a pure lookup.
   *
   * @param model the model to query
   * @return the selected reasoning effort, or {@code null}
   */
  public String getSelectedReasoningEffort(CopilotModel model) {
    if (model == null) {
      return null;
    }
    String key = model.getModelKey();
    UserPreference preference = getUserPreference();
    return preference != null ? preference.getReasoningEffort(key) : null;
  }

  /**
   * Resolves the reasoning effort that should be sent to the language server for the given model: the user's explicit
   * selection when present, otherwise the inferred client-side default from
   * {@link ModelUtils#resolveDefaultReasoningEffort(CopilotModel)}. Returns {@code null} for models that do not
   * support reasoning-effort selection or for the special "auto" model.
   *
   * @param model the model that will receive the request
   * @return the effort to send, or {@code null} to omit
   */
  public String resolveEffectiveReasoningEffort(CopilotModel model) {
    String selected = getSelectedReasoningEffort(model);
    if (StringUtils.isNotBlank(selected)) {
      return selected;
    }
    return ModelUtils.resolveDefaultReasoningEffort(model);
  }

  /**
   * Persists the user-selected reasoning effort for the given model and updates dependent observers.
   *
   * @param model the model to update
   * @param reasoningEffort the reasoning effort to store (may be {@code null} to clear)
   */
  public void setSelectedReasoningEffort(CopilotModel model, String reasoningEffort) {
    if (model == null) {
      return;
    }
    String key = model.getModelKey();
    UserPreference preference = getUserPreference();
    if (preference == null) {
      return;
    }
    preference.setReasoningEffort(key, reasoningEffort);
    CompletableFuture.runAsync(this::persistUserPreference);
    // Publish a fresh snapshot to drive bound picker re-renders. The actual rendering reads
    // resolveEffectiveReasoningEffort (which queries UserPreference), so this observable serves
    // purely as a change signal.
    ensureRealm(() -> reasoningEffortObservable.setValue(preference.getReasoningEffortSnapshot()));
  }

  /**
   * Reconciles the persisted reasoning-effort snapshot with the current model inventory
   * ({@link #copilotModels} ∪ {@link #registeredByokModels}). Entries are kept only when the model still exists and
   * the stored effort is still in that model's advertised reasoning-effort list; everything else is dropped. The
   * map is replaced atomically.
   *
   * <p>Skipped when the inventory is empty (e.g. the very first fetch has not produced results yet or both fetches
   * failed) so a transient outage cannot wipe every stored selection.
   */
  private void reconcileReasoningEfforts() {
    if (copilotModels.isEmpty() && registeredByokModels.isEmpty()) {
      return;
    }
    UserPreference preference = getUserPreference();
    if (preference == null) {
      return;
    }
    Map<String, String> snapshot = preference.getReasoningEffortSnapshot();
    if (snapshot.isEmpty()) {
      return;
    }
    Map<String, CopilotModel> inventory = new HashMap<>();
    for (CopilotModel model : copilotModels.values()) {
      inventory.put(model.getModelKey(), model);
    }
    for (CopilotModel model : registeredByokModels.values()) {
      inventory.put(model.getModelKey(), model);
    }
    Map<String, String> reconciled = new HashMap<>();
    for (Map.Entry<String, String> entry : snapshot.entrySet()) {
      CopilotModel model = inventory.get(entry.getKey());
      if (model == null) {
        continue;
      }
      String stored = entry.getValue();
      for (String effort : ModelUtils.getSupportedReasoningEfforts(model)) {
        if (effort != null && effort.equalsIgnoreCase(stored)) {
          reconciled.put(entry.getKey(), effort);
          break;
        }
      }
    }
    if (preference.setReasoningEfforts(reconciled)) {
      CompletableFuture.runAsync(this::persistUserPreference);
      ensureRealm(() -> reasoningEffortObservable.setValue(preference.getReasoningEffortSnapshot()));
    }
  }

  /**
   * Binds a {@link DropdownButton} to this service for model selection. The button displays model groups with per-item
   * tooltips and billing suffixes.
   *
   * @param picker the dropdown button to bind
   */
  public void bindModelPicker(final DropdownButton picker) {
    // First unbind if previously bound to prevent leaks
    unbindModelPicker(picker);

    // Add the selection listener ONCE here
    picker.setSelectionListener(this::setActiveModel);
    picker.setAccessibilityName("model picker");
    picker.setData("org.eclipse.swtbot.widget.key", "model-picker");

    ensureRealm(() -> {
      ISideEffect modelsSideEffect = ISideEffect.create(() -> {
        Map<String, CopilotModel> modelMap = this.modelObservable.getValue();
        this.reasoningEffortObservable.getValue();
        if (picker.isDisposed() || modelMap.isEmpty()) {
          return Collections.emptyMap();
        }
        return modelMap;
      }, (Map<String, CopilotModel> modelMap) -> rebuildPickerItems(picker, modelMap));

      // Active-model render path: only depends on the active model. The button-face text is read from the matching
      // DropdownItem's selectedLabel (populated by ModelPickerGroupsBuilder with the effective effort), which is
      // refreshed by modelsSideEffect above whenever the reasoning-effort observable changes. There is no need to
      // track the effort observable here.
      ISideEffect activeModelSideEffect = ISideEffect.create(this.activeModelObservable::getValue,
          (CopilotModel activeModel) -> {
            if (activeModel == null || picker.isDisposed()) {
              return;
            }
            picker.setSelectedItemId(activeModel.getModelName());
            String suffix = StringUtils.isNotBlank(activeModel.getDegradationReason())
                ? " - " + activeModel.getDegradationReason() : "";
            picker.setToolTipText(NLS.bind(Messages.chat_actionBar_modelPicker_Tooltip, suffix));
          });

      // Store the side effects for later disposal
      modelButtonSideEffects.put(picker, new ISideEffect[] { modelsSideEffect, activeModelSideEffect });

      // Add a dispose listener to auto-unbind when the combo is disposed
      picker.addDisposeListener(e -> unbindModelPicker(picker));
    });
  }

  /**
   * Rebuilds the item groups for a single picker. Single render path shared by the model-list and
   * reasoning-effort side effects.
   */
  private void rebuildPickerItems(DropdownButton picker, Map<String, CopilotModel> modelMap) {
    if (picker.isDisposed()) {
      return;
    }
    boolean showAddPremiumModelOption = this.authStatusManager.getQuotaStatus()
        .copilotPlan() == CopilotPlan.free;
    // TODO: need to remove this logic after group policy is available
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    boolean showByokManageOption = flags == null || flags.isByokEnabled();
    picker.setItemGroups(ModelPickerGroupsBuilder.build(modelMap, showAddPremiumModelOption, showByokManageOption,
        this::resolveEffectiveReasoningEffort));
  }

  /**
   * Unbind and dispose side effects for a specific DropdownButton.
   *
   * @param picker the dropdown button to unbind
   */
  public void unbindModelPicker(DropdownButton picker) {
    ISideEffect[] effects = modelButtonSideEffects.remove(picker);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  /**
   * Bind the action bar to respond to model vision capability changes.
   *
   * @param actionBar the action bar to bind
   */
  public void bindActionBarForSupportVisionChange(ActionBar actionBar) {
    // First unbind if previously bound to prevent leaks
    unbindActionBarForSupportVisionChange(actionBar);

    ensureRealm(() -> {
      actionBarSideEffect = ISideEffect.create(() -> {
        return isVisionSupported();
      }, (Boolean supportVision) -> {
        if (actionBar.isDisposed()) {
          return;
        }
        actionBar.updateReferencedWidgetsWithSupportVision(supportVision);
      });

      // Add a dispose listener to auto-unbind when the action bar is disposed
      actionBar.addDisposeListener(e -> unbindActionBarForSupportVisionChange(actionBar));
    });
  }

  /**
   * Unbind and dispose side effects for a specific action bar.
   *
   * @param actionBar the action bar to unbind
   */
  public void unbindActionBarForSupportVisionChange(ActionBar actionBar) {
    if (actionBarSideEffect != null) {
      actionBarSideEffect.dispose();
      actionBarSideEffect = null;
    }
  }

  private void disposeAllSideEffects() {
    ensureRealm(() -> {
      for (ISideEffect[] effects : modelButtonSideEffects.values()) {
        for (ISideEffect effect : effects) {
          if (effect != null) {
            effect.dispose();
          }
        }
      }
    });

    modelButtonSideEffects.clear();
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    if (eventBroker != null) {
      eventBroker.unsubscribe(authStatusChangedEventHandler);
      eventBroker.unsubscribe(chatModeChangedEventHandler);
      eventBroker.unsubscribe(byokModelsUpdatedEventHandler);
      eventBroker.unsubscribe(featureFlagsChangedEventHandler);
      eventBroker.unsubscribe(customModeModelChangedEventHandler);
      eventBroker = null;
    }

    // Should not call disposeAllSideEffects here due to issue #1301
    modelButtonSideEffects.clear();
  }
}
