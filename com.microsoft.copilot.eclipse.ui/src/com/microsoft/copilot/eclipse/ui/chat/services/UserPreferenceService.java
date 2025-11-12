package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatMode;
import com.microsoft.copilot.eclipse.core.chat.BuiltInChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.chat.CustomChatModeManager;
import com.microsoft.copilot.eclipse.core.chat.InputNavigation;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.CustomModesPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;

/**
 * Service for managing chat modes and input navigation.
 */
public class UserPreferenceService extends ChatBaseService implements CopilotAuthStatusListener {
  private static final int EXTRA_PADDING = 40;
  private static final String SEPARATOR_PREFIX = "---";
  private IObservableValue<String[]> chatModeObservable;
  private IObservableValue<ChatMode> activeChatModeObservable; // Controls which view to show: Ask or Agent
  private IObservableValue<String> activeModeNameOrIdObservable; // Tracks current mode name/ID for UI elements
  private InputNavigation inputNavigation = new InputNavigation();

  // Track side effects for chat mode combos only
  private final Map<Combo, ISideEffect[]> chatModeComboSideEffects = new HashMap<>();
  private ISideEffect chatViewSideEffect;

  // Event handling
  private IEventBroker eventBroker;
  private EventHandler authStatusChangedEventHandler;
  private EventHandler featureFlagNotifiedEventHandler;

  /**
   * Constructor for the UserPreferenceService.
   */
  public UserPreferenceService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    super(lsConnection, authStatusManager);

    this.authStatusManager.addCopilotAuthStatusListener(this);
    ensureRealm(() -> {
      chatModeObservable = new WritableValue<>(getAvalibleChatModes(), String[].class);
      activeChatModeObservable = new WritableValue<>(null, ChatMode.class);
      activeModeNameOrIdObservable = new WritableValue<>(null, String.class);
    });

    initializeEventHandlers();
    subscribeToEvents();
    init();
  }

  private void initializeEventHandlers() {
    authStatusChangedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof CopilotStatusResult statusResult) {
        // If the user signs out, we need to clear the preference cache to avoid the current preference being used in
        // the next sign in account.
        if (statusResult.isNotSignedIn()) {
          clearUserPreferenceCache();
          this.inputNavigation = null;
        } else {
          // User has signed in - reload built-in modes to ensure we have the latest modes for this user
          try {
            BuiltInChatModeManager.INSTANCE.reloadModes();
            
            // Update available chat modes in the observable to reflect any changes
            ensureRealm(() -> {
              if (!Arrays.deepEquals(getAvalibleChatModes(), chatModeObservable.getValue())) {
                chatModeObservable.setValue(getAvalibleChatModes());
              }
            });
            
            // Reinitialize user preferences for the new user
            init();
          } catch (Exception e) {
            CopilotCore.LOGGER.error("Failed to reload built-in modes on user switch", e);
          }
        }
      }
    };

    featureFlagNotifiedEventHandler = event -> {
      Object property = event.getProperty(IEventBroker.DATA);
      if (property instanceof DidChangeFeatureFlagsParams params) {
        ensureRealm(() -> {
          if (!Arrays.deepEquals(getAvalibleChatModes(), chatModeObservable.getValue())) {
            chatModeObservable.setValue(getAvalibleChatModes());
          }

          if (!params.isAgentModeEnabled()) {
            setActiveChatMode(ChatMode.Ask.toString());
          }
        });
      }
    };
  }

  private void subscribeToEvents() {
    eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    if (eventBroker != null) {
      eventBroker.subscribe(CopilotEventConstants.TOPIC_AUTH_STATUS_CHANGED, authStatusChangedEventHandler);
      eventBroker.subscribe(CopilotEventConstants.TOPIC_CHAT_DID_CHANGE_FEATURE_FLAGS, featureFlagNotifiedEventHandler);
    } else {
      CopilotCore.LOGGER.error(new IllegalStateException("Event broker is null"));
    }
  }

  private void init() {
    if (authStatusManager.isSignedIn()) {
      // Initialize chat mode preferences
      String chatModeName = restoreChatModeName();

      // Determine which view to use (Ask or Agent)
      ChatMode rawViewMode = getViewModeForModeName(chatModeName);

      // Apply feature flags - if agent mode is disabled, force Ask view
      FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
      if (flags != null && !flags.isAgentModeEnabled()) {
        rawViewMode = ChatMode.Ask;
      }

      final ChatMode viewMode = rawViewMode;
      ensureRealm(() -> {
        activeChatModeObservable.setValue(viewMode);
        activeModeNameOrIdObservable.setValue(chatModeName);
      });
      inputNavigation = new InputNavigation(restoreUserInputs());
    }
  }

  /**
   * Restore the chat mode name from preferences.
   *
   * @return the chat mode name or custom mode ID
   */
  public String restoreChatModeName() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getChatModeName() != null) {
      return preference.getChatModeName();
    }

    return ChatMode.Agent.toString();
  }

  /**
   * Determines which UI view (Ask or Agent) should be used for a given mode name.
   * Only the "Ask" built-in mode uses the Ask view; all other modes (Plan, Agent, custom) use the Agent view.
   *
   * @param modeNameOrId the mode name (for built-in modes) or ID (for custom modes)
   * @return ChatMode.Ask or ChatMode.Agent representing which view to render
   */
  private ChatMode getViewModeForModeName(String modeNameOrId) {
    if (StringUtils.isBlank(modeNameOrId)) {
      return ChatMode.Agent; // Default to Agent view
    }

    // Check if it's a custom mode - all custom modes use Agent view
    if (CustomChatModeManager.INSTANCE.isCustomMode(modeNameOrId)) {
      return ChatMode.Agent;
    }

    // Built-in mode - only Ask mode uses Ask view, everything else uses Agent view
    return BuiltInChatMode.ASK_MODE_NAME.equalsIgnoreCase(modeNameOrId) ? ChatMode.Ask : ChatMode.Agent;
  }

  private List<String> restoreUserInputs() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getUserInputs() != null) {
      return preference.getUserInputs(); // Return the most recent input
    }
    return new ArrayList<>();
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
   * Get available chat modes based on feature flags. Includes built-in modes, custom modes, and "Add New Mode" option
   * with separators.
   */
  private String[] getAvalibleChatModes() {
    List<String> modes = new ArrayList<>();

    // Add built-in modes from BuiltInChatModeManager
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    List<BuiltInChatMode> builtInModes = BuiltInChatModeManager.INSTANCE.getBuiltInModes();

    if (flags != null && !flags.isAgentModeEnabled()) {
      // When agent mode is disabled, show only Ask mode
      builtInModes.stream().filter(mode -> mode.getDisplayName().equalsIgnoreCase(BuiltInChatMode.ASK_MODE_NAME))
          .forEach(mode -> modes.add(mode.getDisplayName()));
    } else {
      // Show all built-in modes
      builtInModes.forEach(mode -> modes.add(mode.getDisplayName()));
    }

    // Add custom modes section
    List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();
    if (!customModes.isEmpty()) {
      customModes.forEach(mode -> modes.add(mode.getDisplayName()));
    }

    // Add "Configure Modes" option only if custom agent feature is enabled
    if (FeatureFlags.isCustomAgentEnabled()) {
      modes.add(Messages.configureModes);
    }

    // Calculate the longest mode name to size separators properly
    GC gc = new GC(PlatformUI.getWorkbench().getDisplay());
    try {
      int maxWidth = 0;
      for (String mode : modes) {
        int width = gc.textExtent(mode).x;
        if (width > maxWidth) {
          maxWidth = width;
        }
      }

      // Create separator with proper width
      String separator = createDashSeparator(maxWidth, gc);

      // Insert separators at appropriate positions
      List<String> modesWithSeparators = new ArrayList<>();

      // Calculate built-in mode count for separator placement
      int builtInCount = (flags != null && !flags.isAgentModeEnabled()) ? 1 : builtInModes.size();
      for (int i = 0; i < builtInCount; i++) {
        modesWithSeparators.add(modes.get(i));
      }

      // Add custom modes section with separator
      if (!customModes.isEmpty()) {
        modesWithSeparators.add(separator);
        for (int i = builtInCount; i < builtInCount + customModes.size(); i++) {
          modesWithSeparators.add(modes.get(i));
        }
      }

      // Add "Configure Modes" section with separator only if custom agent feature is enabled
      if (FeatureFlags.isCustomAgentEnabled()) {
        modesWithSeparators.add(separator);
        modesWithSeparators.add(Messages.configureModes);
      }

      return modesWithSeparators.toArray(new String[0]);
    } finally {
      gc.dispose();
    }
  }

  /**
   * Creates a dash separator line matching the width of the content. Similar to ModelService's
   * addDashesAroundModelHeader but for full-width separators.
   *
   * @param maxWidth the maximum width to match
   * @param gc the graphics context for measuring
   * @return a string of dashes matching the width
   */
  private String createDashSeparator(int maxWidth, GC gc) {
    int dashWidth = gc.textExtent("-").x;
    if (dashWidth == 0) {
      dashWidth = 1; // Fallback to prevent division by zero
    }
    int dashCount = maxWidth / dashWidth;
    return "-".repeat(Math.max(1, dashCount));
  }

  /**
   * Set the active chat mode by index from the combo box.
   *
   * @param index the index of the chat mode to set
   * @param combo the combo box containing the modes
   */
  public void setActiveChatMode(int index, Combo combo) {
    if (index < 0 || combo == null || combo.isDisposed()) {
      return;
    }

    String selectedItem = combo.getItem(index);

    // Check if it's a separator - if so, revert to previous selection
    if (selectedItem.startsWith(SEPARATOR_PREFIX)) {
      revertToCurrentMode(combo);
      return;
    }

    // Check if it's "Configure Modes"
    if (selectedItem.equals(Messages.configureModes)) {
      handleConfigureModes(combo);
      return;
    }

    // Check if it's a custom mode
    CustomChatModeManager customModeManager = CustomChatModeManager.INSTANCE;
    CustomChatMode customMode = customModeManager.getCustomModes().stream()
        .filter(mode -> mode.getDisplayName().equals(selectedItem)).findFirst().orElse(null);

    if (customMode != null) {
      // Handle custom mode selection
      setActiveChatMode(customMode.getId());
      return;
    }

    // Check if it's a built-in mode from BuiltInChatModeManager
    List<BuiltInChatMode> builtInModes = BuiltInChatModeManager.INSTANCE.getBuiltInModes();
    for (BuiltInChatMode builtInMode : builtInModes) {
      if (builtInMode.getDisplayName().equals(selectedItem)) {
        // Set active mode using the built-in mode's name directly
        setActiveChatMode(builtInMode.getDisplayName());
        return;
      }
    }

    // If not found, log error
    CopilotCore.LOGGER.info("Selected mode not found: " + selectedItem);
  }

  /**
   * Set the active chat mode by index (legacy method for backward compatibility).
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
    setActiveChatMode(chatModeName);
  }

  /**
   * Set the active chat mode by name or custom mode ID.
   *
   * @param chatModeNameOrId the name/ID of the chat mode to set
   */
  public void setActiveChatMode(String chatModeNameOrId) {
    if (StringUtils.isBlank(chatModeNameOrId)) {
      return;
    }

    // Step 1: Validate that the mode exists
    CustomChatMode customMode = null;
    if (CustomChatModeManager.INSTANCE.isCustomMode(chatModeNameOrId)) {
      customMode = CustomChatModeManager.INSTANCE.getCustomModeById(chatModeNameOrId);
      if (customMode == null) {
        CopilotCore.LOGGER
            .error(new IllegalStateException("Custom mode " + chatModeNameOrId + " no longer exists, ignoring"));
        return;
      }
    }

    // Step 2: Check if already active
    String currentModeName = restoreChatModeName();
    if (chatModeNameOrId.equals(currentModeName)) {
      return;
    }

    // Step 3: Determine which UI view to use (Ask or Agent)
    ChatMode uiViewMode = getViewModeForModeName(chatModeNameOrId);

    // Step 4: Persist user preference
    UserPreference preference = getUserPreference();
    preference.setChatModeName(chatModeNameOrId);
    persistUserPreference();

    // Step 5: Update observables atomically
    final ChatMode finalUiViewMode = uiViewMode;
    ensureRealm(() -> {
      activeChatModeObservable.setValue(finalUiViewMode);
      activeModeNameOrIdObservable.setValue(chatModeNameOrId);
    });

    // Step 6: Post events
    if (eventBroker != null) {
      eventBroker.post(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, uiViewMode);

      // If custom mode has a model defined, publish event to switch to that model
      if (customMode != null && customMode.getModel() != null && !customMode.getModel().isEmpty()) {
        eventBroker.post(CopilotEventConstants.TOPIC_CHAT_CUSTOM_MODE_MODEL_CHANGED, customMode.getModel());
      }
    }
  }

  /**
   * Get the active chat mode (Ask or Agent view).
   *
   * @return the active chat mode for UI rendering
   */
  public ChatMode getActiveChatMode() {
    ChatMode activeChatMode = activeChatModeObservable.getValue();
    if (activeChatMode != null) {
      return activeChatMode;
    }

    // Try to restore from preferences and determine the view mode
    String chatModeName = restoreChatModeName();
    return getViewModeForModeName(chatModeName);
  }

  /**
   * Get the active mode name or custom mode ID from the observable. This returns the current mode being tracked by the
   * observable, which is always in sync with the UI.
   *
   * @return the active mode name (for built-in modes) or custom mode ID (for custom modes)
   */
  public String getActiveModeNameOrId() {
    return activeModeNameOrIdObservable.getValue();
  }

  /**
   * Get the active custom mode if one is selected.
   *
   * @return the custom mode or null if a built-in mode is active
   */
  public CustomChatMode getActiveCustomMode() {
    try {
      String modeName = restoreChatModeName();
      if (CustomChatModeManager.INSTANCE.isCustomMode(modeName)) {
        return CustomChatModeManager.INSTANCE.getCustomModeById(modeName);
      }
    } catch (Exception e) {
      // Ignore and return null
    }
    return null;
  }

  /**
   * Reload the available chat modes from the manager. This will sync custom modes from the Language Server and refresh
   * the dropdown. Built-in modes are loaded once at startup and don't need reloading.
   */
  public void reloadChatModes() {
    // Sync custom modes from LS (built-in modes are loaded once at startup)
    CustomChatModeManager.INSTANCE.syncCustomModesFromService().thenRun(() -> {
      ensureRealm(() -> {
        String[] currentModes = chatModeObservable.getValue();
        String[] updatedModes = getAvalibleChatModes();

        // Only update if the modes have changed
        if (!Arrays.deepEquals(currentModes, updatedModes)) {
          chatModeObservable.setValue(updatedModes);
        }
      });
    });
  }

  /**
   * Revert combo selection to the current active mode. If the current mode no longer exists (e.g., custom mode was
   * deleted), fall back to Ask mode and update the active mode.
   *
   * @param combo the combo box
   */
  private void revertToCurrentMode(Combo combo) {
    // Get the current mode name from preferences (could be custom mode ID or built-in mode name)
    String currentModeName = restoreChatModeName();

    // Check if it's a custom mode
    if (CustomChatModeManager.INSTANCE.isCustomMode(currentModeName)) {
      CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(currentModeName);
      if (customMode != null) {
        // Find the custom mode in the combo by display name
        int currentIndex = Arrays.asList(combo.getItems()).indexOf(customMode.getDisplayName());
        if (currentIndex >= 0) {
          combo.select(currentIndex);
        }
        return;
      } else {
        // Custom mode was deleted, switch to Agent mode
        CopilotCore.LOGGER.info("Current mode " + currentModeName + " was deleted, switching to Agent mode");
        setActiveChatMode(ChatMode.Agent.toString());
        // Select Agent in the combo
        int agentIndex = Arrays.asList(combo.getItems()).indexOf(ChatMode.Agent.displayName());
        if (agentIndex >= 0) {
          combo.select(agentIndex);
        }
        return;
      }
    }

    // It's a built-in mode - find by display name from BuiltInChatModeManager
    BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModes().stream()
        .filter(mode -> mode.getDisplayName().equals(currentModeName)).findFirst().orElse(null);

    if (builtInMode != null) {
      // Find the mode in the combo by display name
      int currentIndex = Arrays.asList(combo.getItems()).indexOf(builtInMode.getDisplayName());
      if (currentIndex >= 0) {
        combo.select(currentIndex);
      }
    } else {
      // Mode not found, default to Agent
      CopilotCore.LOGGER.info("Built-in mode " + currentModeName + " not found, falling back to Agent mode");
      setActiveChatMode(ChatMode.Agent.toString());
      int agentIndex = Arrays.asList(combo.getItems()).indexOf(ChatMode.Agent.displayName());
      if (agentIndex >= 0) {
        combo.select(agentIndex);
      }
    }
  }

  /**
   * Handle the "Configure Modes" selection.
   *
   * @param combo the combo box
   */
  private void handleConfigureModes(Combo combo) {
    // Revert selection first - keep current mode active
    revertToCurrentMode(combo);

    // Open the Custom Modes preference page
    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(combo.getShell(), CustomModesPreferencePage.ID,
        PreferencesUtils.getAllPreferenceIds(), null);

    dialog.open();

    // After dialog closes, reload modes synchronously and restore the current selection
    try {
      CustomChatModeManager customModeManager = CustomChatModeManager.INSTANCE;
      // Wait for sync to complete
      customModeManager.syncCustomModesFromService().join();

      // Update UI on UI thread
      ensureRealm(() -> {
        if (!combo.isDisposed()) {
          // Update the chatModeObservable - this triggers the side effect that calls combo.setItems()
          String[] updatedModes = getAvalibleChatModes();
          chatModeObservable.setValue(updatedModes);

          // Schedule the selection restore to happen after the combo items are set
          // revertToCurrentMode will handle switching to Ask mode if the current mode was deleted
          combo.getDisplay().asyncExec(() -> {
            if (!combo.isDisposed()) {
              revertToCurrentMode(combo);
            }
          });
        }
      });
    } catch (Exception ex) {
      CopilotCore.LOGGER.error("Failed to reload modes after configuration", ex);
    }
  }

  /**
   * Bind a chat mode picker combo to this service.
   *
   * @param combo the combo to bind
   */
  public void bindChatModePicker(final Combo combo) {
    // First unbind if previously bound to prevent leaks
    unbindChatModePicker(combo);

    ensureRealm(() -> {
      ISideEffect chatModesSideEffect = ISideEffect.create(() -> {
        return this.chatModeObservable.getValue();
      }, (String[] chatModes) -> {
        if (!combo.isDisposed()) {
          combo.setItems(chatModes);
          if (chatModes.length == 1) {
            combo.select(0);
          } else {
            // Restore the current selection after updating items
            String currentModeNameOrId = this.activeModeNameOrIdObservable.getValue();
            if (currentModeNameOrId != null) {
              restoreComboSelection(combo, currentModeNameOrId);
            }
          }
        }
      });

      ISideEffect activeChatModeSideEffect = ISideEffect.create(() -> {
        // Return the current mode name or ID from observable
        String modeNameOrId = this.activeModeNameOrIdObservable.getValue();
        if (modeNameOrId == null) {
          // Fallback to restoring from preferences
          modeNameOrId = restoreChatModeName();
        }
        return modeNameOrId;
      }, (String modeNameOrId) -> {
        if (combo.isDisposed() || modeNameOrId == null) {
          return;
        }
        restoreComboSelection(combo, modeNameOrId);
      });

      // Store the side effects for later disposal
      chatModeComboSideEffects.put(combo, new ISideEffect[] { chatModesSideEffect, activeChatModeSideEffect });

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
   * Bind the chat view to automatically switch between Ask and Agent layouts when the active mode changes.
   * This creates a side effect that rebuilds the view whenever activeChatModeObservable changes.
   */
  public void bindChatView(ChatView chatView) {
    if (chatView == null) {
      return;
    }

    // Unbind any previously bound chat view
    unbindChatView();

    // Create a side effect that watches activeChatModeObservable and rebuilds the view
    ensureRealm(() -> chatViewSideEffect = ISideEffect.create(() -> {
      return this.activeChatModeObservable.getValue();
    }, (ChatMode viewMode) -> {
      if (viewMode != null) {
        // Rebuild the view for Ask or Agent layout
        chatView.buildViewFor(viewMode);
      }
    }));
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
   * Restore the combo selection based on the current mode name or ID.
   *
   * @param combo the combo to restore selection for
   *
   * @param modeNameOrId the current mode name or custom mode ID
   */
  private void restoreComboSelection(Combo combo, String modeNameOrId) {
    String displayName;

    // Check if it's a custom mode ID
    if (CustomChatModeManager.INSTANCE.isCustomMode(modeNameOrId)) {
      CustomChatMode customMode = CustomChatModeManager.INSTANCE.getCustomModeById(modeNameOrId);
      if (customMode != null) {
        displayName = customMode.getDisplayName();
      } else {
        // Custom mode no longer exists, fall back to Ask
        displayName = ChatMode.Ask.displayName();
      }
    } else {
      // It's a built-in mode - look it up by display name
      BuiltInChatMode builtInMode = BuiltInChatModeManager.INSTANCE.getBuiltInModes().stream()
          .filter(mode -> mode.getDisplayName().equals(modeNameOrId)).findFirst().orElse(null);

      if (builtInMode != null) {
        displayName = builtInMode.getDisplayName();
      } else {
        // Mode not found, fall back to Ask
        displayName = ChatMode.Ask.displayName();
      }
    }

    int index = Arrays.asList(combo.getItems()).indexOf(displayName);
    if (index >= 0) {
      updateSelectedItem(combo, displayName, index);
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

    // Always update and force layout, especially important when shrinking (e.g., switching to "Ask")
    gridData.widthHint = widthHint;
    combo.requestLayout();
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
    if (inputNavigation == null) {
      return;
    }

    inputNavigation.updateCursorPosition(inputNavigation.size());
  }

  /**
   * Gets whether to skip the GitHub Job confirmation dialog.
   *
   * @return true if the dialog should be skipped, false otherwise
   */
  public boolean isSkipGitHubJobConfirmDialog() {
    UserPreference preference = getUserPreference();
    return preference != null && preference.isSkipGitHubJobConfirmDialog();
  }

  /**
   * Sets whether to skip the GitHub Job confirmation dialog.
   *
   * @param skip true to skip the dialog, false otherwise
   */
  public void setSkipGitHubJobConfirmDialog(boolean skip) {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.isSkipGitHubJobConfirmDialog() != skip) {
      preference.setSkipGitHubJobConfirmDialog(skip);
      persistUserPreference();
    }
  }

  /**
   * Dispose of the service.
   */
  public void dispose() {
    persistUserPreference();
    // Ideally we should dispose all side effects and observable here. But since the service is
    // singleton and will only be disposed when the bundle is stopped. So right now they are not
    // explicitly disposed here.
    this.authStatusManager.removeCopilotAuthStatusListener(this);

    if (eventBroker != null) {
      eventBroker.unsubscribe(authStatusChangedEventHandler);
      eventBroker.unsubscribe(featureFlagNotifiedEventHandler);
      authStatusChangedEventHandler = null;
      featureFlagNotifiedEventHandler = null;
      eventBroker = null;
    }
  }

  private void disposeAllSideEffects() {
    ensureRealm(() -> {
      // Dispose chat mode combo side effects
      for (ISideEffect[] effects : chatModeComboSideEffects.values()) {
        for (ISideEffect effect : effects) {
          if (effect != null) {
            effect.dispose();
          }
        }
      }
    });

    chatModeComboSideEffects.clear();
  }
}
