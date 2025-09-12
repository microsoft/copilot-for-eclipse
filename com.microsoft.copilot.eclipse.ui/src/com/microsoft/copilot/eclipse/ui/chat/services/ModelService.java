package com.microsoft.copilot.eclipse.ui.chat.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.handlers.OpenPreferencesHandler;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.ChatPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CompletionsPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CopilotPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.CustomInstructionPreferencePage;
import com.microsoft.copilot.eclipse.ui.preferences.GeneralPreferencesPage;
import com.microsoft.copilot.eclipse.ui.preferences.McpPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Service for managing AI models and their selection. Handles all model-related functionality including persistence,
 * BYOK integration, UI binding, and communicates with other services through pure events.
 */
public class ModelService extends ChatBaseService {

  /**
   * The extra padding that used for the combo on non-Windows platforms.
   */
  private static final int EXTRA_PADDING = 40;
  private static final String MODEL_MULTIPLIER_SUFFIX = "x";
  private static final String DEFAULT_MODEL_MULTIPLIER = "Included";
  private static final int MIN_WIDTH_BETWEEN_MODEL_NAME_AND_MULTIPLIER = 6;

  // models for the model picker
  private IObservableValue<Map<String, CopilotModel>> modelObservable;
  private IObservableValue<CopilotModel> activeModelObservable;
  // Used to update modelObservable
  private Map<String, CopilotModel> copilotModels = new HashMap<>();
  private Map<String, CopilotModel> registeredByokModels = new HashMap<>();
  private CopilotModel defaultModel;
  private CopilotModel fallbackModel;

  private ChatMode currentChatMode = ChatMode.Agent;

  // Track side effects for each model combo
  private final Map<Combo, ISideEffect[]> modelComboSideEffects = new HashMap<>();
  private ISideEffect actionBarSideEffect;

  // Event communication
  private IEventBroker eventBroker;
  private EventHandler authStatusChangedEventHandler;
  private EventHandler chatModeChangedEventHandler;
  private EventHandler byokModelsUpdatedEventHandler;

  /**
   * Constructor for the ModelService.
   */
  public ModelService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    super(lsConnection, authStatusManager);

    ensureRealm(() -> {
      modelObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      activeModelObservable = new WritableValue<>(null, CopilotModel.class);
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
        ensureRealm(() -> updateModelsForChatMode(currentChatMode));
      }
    };
  }

  private void subscribeToEvents() {
    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, authStatusChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, chatModeChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_BYOK_MODELS_UPDATED, byokModelsUpdatedEventHandler);
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
      boolean supportsChat = model.getScopes().contains(CopilotScope.CHAT_PANEL);
      boolean supportsAgent = model.getScopes().contains(CopilotScope.AGENT_PANEL);
      if (supportsChat || supportsAgent) {
        newModels.put(model.getId(), model);
      }
      if (defaultModel == null && model.isChatDefault()) {
        defaultModel = model;
      }
      if (fallbackModel == null && model.isChatFallback()) {
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

    return defaultModel.getId();
  }

  private void updateModelsForChatMode(ChatMode chatMode) {
    String scope = modeToScope(chatMode);

    // Filter models for the current mode from combined models
    final Map<String, CopilotModel> modelsForCurrentMode = new HashMap<>();
    Map<String, CopilotModel> allModels = new HashMap<>();
    allModels.putAll(copilotModels);
    allModels.putAll(registeredByokModels);

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
    boolean isCurrentModelAvailable = false;
    if (currentActive != null) {
      String keyToFind = currentActive.getProviderName() != null
          ? currentActive.getProviderName() + "_" + currentActive.getId()
          : currentActive.getId();
      isCurrentModelAvailable = modelsForCurrentMode.containsKey(keyToFind);
    }
    if (currentActive == null || !isCurrentModelAvailable) {
      // Try to restore user's preferred model if it's available in current mode
      String restoredModelId = restoreActiveModel();
      if (restoredModelId != null && modelsForCurrentMode.containsKey(restoredModelId)) {
        ensureRealm(() -> activeModelObservable.setValue(modelsForCurrentMode.get(restoredModelId)));
        return;
      }
      // fall back to default model
      setActiveModelToDefault();
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
      persistUserPreference();

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

  private void setActiveModelToDefault() {
    if (defaultModel != null) {
      UserPreference preference = getUserPreference();
      preference.setChatModel(defaultModel.getId());
      persistUserPreference();

      ensureRealm(() -> activeModelObservable.setValue(defaultModel));
    }
  }

  /**
   * Register a side effect for the given Combo when the model names change.
   *
   * @param combo the combo to set the items
   */
  public void bindModelPicker(final Combo combo) {
    // First unbind if previously bound to prevent leaks
    unbindModelPicker(combo);

    // Add the selection listener ONCE here
    combo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int index = combo.getSelectionIndex();
        if (index >= 0 && index < combo.getItemCount()) {
          String modelNameWithMultiplier = combo.getItem(index);
          String trimmedModelNameWithMultiplier = modelNameWithMultiplier.replace("-", "");
          if (trimmedModelNameWithMultiplier.equals(Messages.chat_standardModels)
              || trimmedModelNameWithMultiplier.equals(Messages.chat_premiumModels)
              || trimmedModelNameWithMultiplier.equals(Messages.chat_copilotModels)
              || trimmedModelNameWithMultiplier.equals(Messages.chat_customModels)
              || StringUtils.isBlank(trimmedModelNameWithMultiplier)) {
            dismissComboSelection(e, combo);
          } else if (trimmedModelNameWithMultiplier.equals(Messages.chat_addPremiumModels)) {
            dismissComboSelection(e, combo);
            UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan", null);
          } else if (trimmedModelNameWithMultiplier.equals(Messages.chat_actionBar_modelPicker_manageModels)) {
            dismissComboSelection(e, combo);
            Map<String, Object> parameters = new HashMap<>();

            parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.activePageId",
                ByokPreferencePage.ID);

            parameters.put("com.microsoft.copilot.eclipse.commands.openPreferences.pageIds",
                String.join(",", CopilotPreferencesPage.ID, GeneralPreferencesPage.ID, ChatPreferencesPage.ID,
                    CompletionsPreferencesPage.ID, CustomInstructionPreferencePage.ID, McpPreferencePage.ID,
                    ByokPreferencePage.ID));

            UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.openPreferences", parameters);
          } else {
            setActiveModel(getModelNameFromModelWithMultiplier(modelNameWithMultiplier));
          }
        }
      }
    });

    ensureRealm(() -> {
      ISideEffect modelNamesSideEffect = ISideEffect.create(() -> {
        Map<String, CopilotModel> modelMap = this.modelObservable.getValue();
        if (combo.isDisposed() || modelMap.isEmpty()) {
          return new String[0];
        }
        return composeModelList(combo, modelMap);
      }, (String[] modelNames) -> {
        if (!combo.isDisposed()) {
          combo.setItems(modelNames);
          updateSelectionForActiveModel(combo);
        }
      });

      ISideEffect activeModelSideEffect = ISideEffect.create(() -> {
        return this.activeModelObservable.getValue();
      }, (CopilotModel activeModel) -> {
        if (activeModel == null || combo.isDisposed()) {
          return;
        }
        int index = getModelIndexFromComboByModelName(combo, activeModel.getModelName());
        if (index >= 0) {
          updateSelectedItem(combo, activeModel.getModelName(), index);
        }
      });

      // Store the side effects for later disposal
      modelComboSideEffects.put(combo, new ISideEffect[] { modelNamesSideEffect, activeModelSideEffect });

      // Add a dispose listener to auto-unbind when the combo is disposed
      combo.addDisposeListener(e -> unbindModelPicker(combo));
    });
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

  private String[] composeModelList(Combo combo, Map<String, CopilotModel> modelMap) {
    // Get font metrics for proper alignment
    GC gc = new GC(combo);

    try {
      // Calculate the width of each model name
      Map<String, Integer> formattedModelWidths = new HashMap<>();
      int maxWidth = 0;

      for (CopilotModel model : modelMap.values()) {
        int width = calculateModelDisplayWidth(gc, model);
        formattedModelWidths.put(model.getModelName(), width);
        maxWidth = Math.max(maxWidth, width);
      }

      // Calculate width of a hair space character
      int spaceWidth = gc.textExtent(UiUtils.HAIR_SPACE).x;

      // Create properly aligned model names
      List<String> standardModels = new ArrayList<>();
      List<String> premiumModels = new ArrayList<>();
      List<String> customModels = new ArrayList<>();

      for (CopilotModel model : modelMap.values()) {
        String formattedModel = formatModelWithAlignment(gc, model, formattedModelWidths, maxWidth, spaceWidth);

        if (model.getProviderName() != null) {
          customModels.add(formattedModel);
          continue;
        }
        if (model.getBilling().isPremium()) {
          premiumModels.add(formattedModel);
        } else {
          standardModels.add(formattedModel);
        }
      }
      if (!customModels.isEmpty()) {
        customModels.sort(String.CASE_INSENSITIVE_ORDER);
        customModels.add(0, addDashesAroundModelHeader(Messages.chat_customModels, maxWidth, gc));
      }

      if (!standardModels.isEmpty()) {
        standardModels.sort(String.CASE_INSENSITIVE_ORDER);
        standardModels.add(0, addDashesAroundModelHeader(Messages.chat_standardModels, maxWidth, gc));
      }
      if (!premiumModels.isEmpty()) {
        premiumModels.sort(String.CASE_INSENSITIVE_ORDER);
        if (standardModels.isEmpty()) {
          premiumModels.add(0, addDashesAroundModelHeader(Messages.chat_copilotModels, maxWidth, gc));
        } else {
          premiumModels.add(0, addDashesAroundModelHeader(Messages.chat_premiumModels, maxWidth, gc));
        }
      }

      List<String> allModels = new ArrayList<>(standardModels);
      allModels.addAll(premiumModels);
      allModels.addAll(customModels);
      if (this.authStatusManager.getQuotaStatus().getCopilotPlan() == CopilotPlan.free) {
        allModels.add(addDashesAroundModelHeader("", maxWidth, gc));
        allModels.add(Messages.chat_addPremiumModels);
      }
      allModels.add(addDashesAroundModelHeader("", maxWidth, gc));
      allModels.add(Messages.chat_actionBar_modelPicker_manageModels);
      return allModels.toArray(new String[0]);
    } finally {
      gc.dispose();
    }
  }

  /**
   * Formats a model name with its multiplier, adding spacing for alignment.
   */
  private String formatModelWithAlignment(GC gc, CopilotModel model, Map<String, Integer> modelWidths, int maxWidth,
      int spaceWidth) {
    String modelName = model.getModelName();
    int currentWidth = modelWidths.get(modelName);
    int spacesToAdd = (int) Math.round((maxWidth - currentWidth) / (double) spaceWidth) + 1;

    String suffix = "";
    if (model.getProviderName() != null) {
      suffix = model.getProviderName();
    } else if (model.getBilling() != null) {
      BigDecimal multiplier = BigDecimal.valueOf(model.getBilling().multiplier()).stripTrailingZeros();
      if (multiplier.toPlainString().equals("0")) {
        suffix = DEFAULT_MODEL_MULTIPLIER;
      } else {
        suffix = multiplier.toPlainString() + MODEL_MULTIPLIER_SUFFIX;
      }
    }

    return UiUtils.getAlignedText(gc, modelName, UiUtils.HAIR_SPACE, suffix, spacesToAdd, maxWidth);
  }

  /**
   * Surrounds the model header text with dashes to create a visual separator.
   */
  private String addDashesAroundModelHeader(String modelHeader, int maxWidth, GC gc) {
    int headerWidth = gc.textExtent(modelHeader).x;
    int dashWidth = gc.textExtent("-").x;

    int dashesToAdd = (int) (Math.round((maxWidth - headerWidth) / (double) dashWidth) + 1) / 2;

    return "-".repeat(dashesToAdd) + modelHeader + "-".repeat(dashesToAdd);
  }

  /**
   * Calculates the display width of a model item including its name and multiplier text.
   */
  private int calculateModelDisplayWidth(GC gc, CopilotModel model) {
    String multiplierText;

    // Handle BYOK models (which have providerName but no billing)
    if (model.getProviderName() != null) {
      multiplierText = model.getProviderName();
    } else if (model.getBilling() != null) {
      BigDecimal multiplier = BigDecimal.valueOf(model.getBilling().multiplier()).stripTrailingZeros();
      if (multiplier.toPlainString().equals("0")) {
        multiplierText = DEFAULT_MODEL_MULTIPLIER;
      } else {
        multiplierText = multiplier.toPlainString() + MODEL_MULTIPLIER_SUFFIX;
      }
    } else {
      // Fallback for models without billing info
      multiplierText = "";
    }

    return gc.textExtent(model.getModelName() + multiplierText).x + MIN_WIDTH_BETWEEN_MODEL_NAME_AND_MULTIPLIER;
  }

  /**
   * Helper method to update selection based on active model.
   */
  private void updateSelectionForActiveModel(Combo combo) {
    CopilotModel activeModel = this.activeModelObservable.getValue();
    String modelName = null;

    if (activeModel != null) {
      modelName = activeModel.getModelName();
    } else if (defaultModel != null) {
      modelName = defaultModel.getModelName();
    }

    if (modelName != null && combo.getItemCount() > 0) {
      int index = getModelIndexFromComboByModelName(combo, modelName);
      if (index >= 0) {
        updateSelectedItem(combo, modelName, index);
      }
    }
  }

  private void dismissComboSelection(SelectionEvent e, Combo combo) {
    // Prevent selection of header items
    e.doit = false;
    updateSelectionForActiveModel(combo);
  }

  private int getModelIndexFromComboByModelName(Combo combo, String modelName) {
    return Arrays.stream(combo.getItems())
        .map(modelNameWithMultiplier -> getModelNameFromModelWithMultiplier(modelNameWithMultiplier)).toList()
        .indexOf(modelName);
  }

  private String getModelNameFromModelWithMultiplier(String modelNameWithMultiplier) {
    return modelNameWithMultiplier.split(UiUtils.HAIR_SPACE)[0].trim();
  }

  /**
   * Unbind and dispose side effects for a specific combo.
   *
   * @param combo the combo to unbind
   */
  public void unbindModelPicker(Combo combo) {
    ISideEffect[] effects = modelComboSideEffects.remove(combo);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  private void updateSelectedItem(final Combo combo, String itemName, int index) {
    combo.select(index);
    // adjust the width according to the item
    GC gc = new GC(combo);
    Point textExtent = gc.textExtent(itemName);
    gc.dispose();

    GridData gridData = (GridData) combo.getLayoutData();
    // Add some padding (dropdown button width + horizontal margins)
    int padding = PlatformUtils.isWindows() ? 0 : EXTRA_PADDING;
    int widthHint = textExtent.x + padding;
    if (gridData.widthHint != widthHint) {
      gridData.widthHint = widthHint;
      combo.requestLayout();
    }
  }

  private void disposeAllSideEffects() {
    ensureRealm(() -> {
      for (ISideEffect[] effects : modelComboSideEffects.values()) {
        for (ISideEffect effect : effects) {
          if (effect != null) {
            effect.dispose();
          }
        }
      }
    });

    modelComboSideEffects.clear();

  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    if (eventBroker != null) {
      eventBroker.unsubscribe(authStatusChangedEventHandler);
      eventBroker.unsubscribe(chatModeChangedEventHandler);
      eventBroker.unsubscribe(byokModelsUpdatedEventHandler);
      eventBroker = null;
    }

    disposeAllSideEffects();
  }
}
