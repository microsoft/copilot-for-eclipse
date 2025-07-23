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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.InputNavigation;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotScope;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ActionBar;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manager for chat services.
 */
public class UserPreferenceService extends ChatBaseService implements CopilotAuthStatusListener {
  /**
   * The extra padding that used for the combo on non-Windows platforms.
   */
  private static final int EXTRA_PADDING = 40;
  private static final String MODEL_MULTIPLIER_SUFFIX = "x";
  private static final String DEFAULT_MODEL_MULTIPLIER = "Included";
  private static final int MIN_WIDTH_BETWEEN_MODEL_NAME_AND_MULTIPLIER = 6;

  // data
  private IObservableValue<ChatMode> activeChatModeObservable;
  private IObservableValue<Map<String, CopilotModel>> modelObservable;
  private IObservableValue<CopilotModel> activeModelObservable;
  private Map<String, CopilotModel> models = new HashMap<>();
  private CopilotModel defaultModel;
  private CopilotModel fallbackModel;
  private InputNavigation inputNavigation = new InputNavigation();

  // Track side effects for each combo
  private final Map<Combo, ISideEffect[]> modelComboSideEffects = new HashMap<>();
  private final Map<Combo, ISideEffect[]> chatModeComboSideEffects = new HashMap<>();
  private ISideEffect actionBarSideEffect;
  private ISideEffect chatViewSideEffect;

  /**
   * Constructor for the CopilotModelService.
   */
  public UserPreferenceService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    super(lsConnection, authStatusManager);

    this.authStatusManager.addCopilotAuthStatusListener(this);
    ensureRealm(() -> {
      activeChatModeObservable = new WritableValue<>(null, ChatMode.class);
      modelObservable = new WritableValue<>(new HashMap<>(), HashMap.class);
      activeModelObservable = new WritableValue<>(null, CopilotModel.class);
      ISideEffect.create(() -> {
        ChatMode mode = activeChatModeObservable.getValue();
        final Map<String, CopilotModel> modelsForCurrentMode = new HashMap<>();
        String scope = modeToScope(mode);
        for (CopilotModel model : this.models.values()) {
          if (model.getScopes().contains(scope)) {
            modelsForCurrentMode.put(model.getId(), model);
          }
        }
        return modelsForCurrentMode;
      }, (Map<String, CopilotModel> currentModels) -> {
        modelObservable.setValue(currentModels);
        updateActiveModelForModeChange();
      });
    });
    init();
  }

  private void init() {
    if (authStatusManager.isSignedIn()) {
      Job job = new Job("Fetching model list...") {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            // fetch the models
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
            models = newModels;

            restoreFromUserPreference();
          } catch (InterruptedException | ExecutionException e) {
            CopilotCore.LOGGER.error("Failed to list models", e);
          }
          return Status.OK_STATUS;
        }
      };
      job.setSystem(true);
      job.schedule();
    }
  }

  private void restoreFromUserPreference() {
    // restore the chat mode
    String chatModeName = restoreChatModeName();
    final ChatMode chatMode = ChatMode.valueOf(chatModeName);
    ensureRealm(() -> activeChatModeObservable.setValue(chatMode));

    // restore the model list for current chat mode
    final Map<String, CopilotModel> modelsForCurrentMode = new HashMap<>();
    String scope = modeToScope(chatMode);
    for (CopilotModel model : this.models.values()) {
      if (model.getScopes().contains(scope)) {
        modelsForCurrentMode.put(model.getId(), model);
      }
    }
    ensureRealm(() -> modelObservable.setValue(modelsForCurrentMode));

    // restore the active model
    final String modelId = restoreModelId();
    CopilotModel model = models.get(modelId);
    if (!ensureModelCanBeEnabled(model, chatMode)) {
      model = defaultModel;
    }
    final CopilotModel finalModel = model;
    ensureRealm(() -> activeModelObservable.setValue(finalModel));

    // restore the input history
    inputNavigation = new InputNavigation(restoreUserInputs());
  }

  private String restoreModelId() {
    // TODO: check if the model name is in modelMap
    // get the path for the chat persistence
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getChatModel() != null) {
      return preference.getChatModel();
    }

    return defaultModel.getId();
  }

  private String restoreChatModeName() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getChatModeName() != null) {
      return preference.getChatModeName();
    }

    return ChatMode.Agent.toString();
  }

  private List<String> restoreUserInputs() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getUserInputs() != null) {
      return preference.getUserInputs(); // Return the most recent input
    }
    return new ArrayList<>();
  }

  private boolean ensureModelCanBeEnabled(CopilotModel model, ChatMode chatMode) {
    String scope = modeToScope(chatMode);
    return !(model == null || !model.getScopes().contains(scope));
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

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    String status = copilotStatusResult.getStatus();
    switch (status) {
      case CopilotStatusResult.OK, CopilotStatusResult.NOT_AUTHORIZED:
        init();
        break;
      default:
        disposeAllSideEffects();
        break;
    }
  }

  /**
   * Set the active chat mode.
   *
   * @param index the index of the chat mode to set
   */
  public void setActiveChatMode(int index) {
    ChatMode[] modes = ChatMode.values();
    // Only update if the selected mode is different from the current mode
    if (index < 0 || index >= modes.length) {
      return;
    }

    String chatModeName = modes[index].toString();
    // Persist the chat mode selection
    UserPreference preference = getUserPreference();
    preference.setChatModeName(chatModeName);
    persistUserPreference();

    ensureRealm(() -> activeChatModeObservable.setValue(ChatMode.valueOf(chatModeName)));
  }

  /**
   * Get the active chat mode.
   *
   * @return the active chat mode
   */
  public ChatMode getActiveChatMode() {
    ChatMode activeChatMode = activeChatModeObservable.getValue();
    return activeChatMode == null ? ChatMode.valueOf(restoreChatModeName()) : activeChatMode;
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
   * Check if active model is valid for the current mode, otherwise set the default model.
   *
   * @param model the current active model
   */
  private void updateActiveModelForModeChange() {
    CopilotModel model = activeModelObservable.getValue();
    if (ensureModelCanBeEnabled(model, getActiveChatMode())) {
      return;
    }
    if (defaultModel != null) {
      UserPreference preference = getUserPreference();
      preference.setChatModel(defaultModel.getId());
      persistUserPreference();
      ensureRealm(() -> {
        activeModelObservable.setValue(defaultModel);
      });
    }
  }

  /**
   * Set the active model.
   *
   * @param modelName the name of the model
   */
  public void setActiveModel(String modelName) {
    CopilotModel model = modelObservable.getValue().values().stream().filter(m -> m.getModelName().equals(modelName))
        .findFirst().orElse(null);

    if (model == null || !ensureModelCanBeEnabled(model, getActiveChatMode())) {
      return;
    }

    // Try to remember the model name
    UserPreference preference = getUserPreference();
    preference.setChatModel(model.getId());
    persistUserPreference();

    ensureRealm(() -> activeModelObservable.setValue(model));
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
   * Check if the active model supports vision capabilities.
   */
  public boolean isVisionSupported() {
    CopilotModel model = getActiveModel();
    return model != null && model.getCapabilities().supports().vision();
  }

  /**
   * Bind a chat mode picker combo to this service.
   *
   * @param combo the combo to bind
   */
  public void bindChatModePicker(final Combo combo) {
    // First unbind if previously bound to prevent leaks
    unbindChatModePicker(combo);

    SwtUtils.invokeOnDisplayThread(() -> {
      if (combo.isDisposed()) {
        return;
      }
      String[] items = Arrays.stream(ChatMode.values()).map(ChatMode::displayName).toArray(String[]::new);
      combo.setItems(items);
    }, combo);

    ensureRealm(() -> {
      ISideEffect activeChatModeSideEffect = ISideEffect.create(() -> {
        ChatMode activeMode = this.activeChatModeObservable.getValue();
        return activeMode == null ? ChatMode.valueOf(restoreChatModeName()) : activeMode;
      }, (ChatMode mode) -> {
        if (combo.isDisposed()) {
          return;
        }
        String modeName = mode.displayName();
        int index = Arrays.asList(combo.getItems()).indexOf(modeName);
        if (index >= 0) {
          updateSelectedItem(combo, modeName, index);
        }
      });

      // Store the side effects for later disposal
      chatModeComboSideEffects.put(combo, new ISideEffect[] { activeChatModeSideEffect });

      // Add a dispose listener to auto-unbind when the combo is disposed
      combo.addDisposeListener(e -> unbindChatModePicker(combo));
    });
  }

  /**
   * Unbind and dispose side effects for a specific combo.
   *
   * @param combo the combo to unbind
   */
  public void unbindChatModePicker(Combo combo) {
    ISideEffect[] effects = chatModeComboSideEffects.remove(combo);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  /**
   * Bind the chat view to the chat mode.
   */
  public void bindChatView(ChatView chatView) {
    if (chatView == null) {
      return;
    }

    // Unbind any previously bound chat view
    unbindChatView();

    ensureRealm(() -> chatViewSideEffect = ISideEffect.create(() -> {
      return this.activeChatModeObservable.getValue();
    }, chatView::buildViewFor));
  }

  /**
   * Unbind the currently bound chat view if any.
   */
  public void unbindChatView() {
    if (chatViewSideEffect != null) {
      chatViewSideEffect.dispose();
      chatViewSideEffect = null;
    }
  }

  /**
   * Register a side effect for the given Combo when the model names.
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
              || StringUtils.isBlank(trimmedModelNameWithMultiplier)) {
            dismissComboSelection(e, combo);
          } else if (trimmedModelNameWithMultiplier.equals(Messages.chat_addPremiumModels)) {
            dismissComboSelection(e, combo);
            UiUtils.executeCommandWithParameters("com.microsoft.copilot.eclipse.commands.upgradeCopilotPlan", null);
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
        if (modelMap.values().stream().anyMatch(m -> m.getBilling() == null)) {
          // TODO: this case can be removed when all models from CLS have billing info
          String[] names = modelMap.values().stream().map(CopilotModel::getModelName).toArray(String[]::new);
          Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
          return names;
        } else {
          return composeModelList(combo, modelMap);
        }
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

      for (CopilotModel model : modelMap.values()) {
        String formattedModel = formatModelWithAlignment(gc, model, formattedModelWidths, maxWidth, spaceWidth);

        if (model.getBilling().isPremium()) {
          premiumModels.add(formattedModel);
        } else {
          standardModels.add(formattedModel);
        }
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
      if (this.authStatusManager.getQuotaStatus().getCopilotPlan() == CopilotPlan.free) {
        allModels.add(addDashesAroundModelHeader("", maxWidth, gc));
        allModels.add(Messages.chat_addPremiumModels);
      }
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
    BigDecimal multiplier = BigDecimal.valueOf(model.getBilling().multiplier()).stripTrailingZeros();
    if (multiplier.toPlainString().equals("0")) {
      suffix = DEFAULT_MODEL_MULTIPLIER;
    } else {
      suffix = multiplier.toPlainString() + MODEL_MULTIPLIER_SUFFIX;
    }

    return UiUtils.getAlignedText(gc, modelName, UiUtils.HAIR_SPACE, suffix, spacesToAdd, maxWidth);
  }

  /**
   * Surrounds the model header text with dashes to create a visual separator. The total width will be approximately the
   * same as maxWidth.
   */
  private String addDashesAroundModelHeader(String modelHeader, int maxWidth, GC gc) {
    int headerWidth = gc.textExtent(modelHeader).x;
    int dashWidth = gc.textExtent("-").x;

    // Calculate how many dashes to add on each side to make total width similar to maxWidth
    // We divide by 2 to distribute dashes evenly on both sides
    int dashesToAdd = (int) (Math.round((maxWidth - headerWidth) / (double) dashWidth) + 1) / 2;

    // Build the string with dashes on both sides
    return "-".repeat(dashesToAdd) + modelHeader + "-".repeat(dashesToAdd);
  }

  /**
   * Calculates the display width of a model item including its name and multiplier text.
   */
  private int calculateModelDisplayWidth(GC gc, CopilotModel model) {
    // Format the multiplier text once
    String multiplierText;
    BigDecimal multiplier = BigDecimal.valueOf(model.getBilling().multiplier()).stripTrailingZeros();

    if (multiplier.toPlainString().equals("0")) {
      multiplierText = DEFAULT_MODEL_MULTIPLIER;
    } else {
      multiplierText = multiplier.toPlainString() + MODEL_MULTIPLIER_SUFFIX;
    }

    // Calculate total width
    return gc.textExtent(model.getModelName() + multiplierText).x + MIN_WIDTH_BETWEEN_MODEL_NAME_AND_MULTIPLIER;
  }

  /**
   * Helper method to update selection based on active model.
   */
  private void updateSelectionForActiveModel(Combo combo) {
    CopilotModel activeModel = this.activeModelObservable.getValue();
    ChatMode activeMode = this.activeChatModeObservable.getValue();
    String modelName = null;

    if (ensureModelCanBeEnabled(activeModel, activeMode)) {
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

  /**
   * Add input to the input history.
   */
  public void addInputToHistory(String input) {
    inputNavigation.add(input);
    UserPreference preference = getUserPreference();
    preference.setUserInputs(inputNavigation.getInputHistoryList());
  }

  /**
   * Get the previous input from the input history.
   *
   * @param currentInput the current input to check if it should be added to history.
   * @return the previous input or an empty string if at the top of the history.
   */
  public String getPreviousInput(String currentInput) {
    if (inputNavigation.atBottom() && StringUtils.isNotEmpty(currentInput)) {
      inputNavigation.add(currentInput);
      inputNavigation.updateCursorPosition(inputNavigation.size() - 1);
    }
    return inputNavigation.navigateUp();
  }

  public String getNextInput() {
    return inputNavigation.navigateDown();
  }

  /**
   * Reset the input history cursor to the latest input.
   */
  public void resetInputHistoryCursor() {
    inputNavigation.updateCursorPosition(inputNavigation.size());
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    persistUserPreference();
    // Ideally we should dispose all side effects and observable here. But since the service is
    // singleton and will only be disposed when the bundle is stopped. So right now they are not
    // explicitly disposed here.
    this.authStatusManager.removeCopilotAuthStatusListener(this);
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

      for (ISideEffect[] effects : chatModeComboSideEffects.values()) {
        for (ISideEffect effect : effects) {
          if (effect != null) {
            effect.dispose();
          }
        }
      }
    });

    modelComboSideEffects.clear();
    chatModeComboSideEffects.clear();
  }

}
