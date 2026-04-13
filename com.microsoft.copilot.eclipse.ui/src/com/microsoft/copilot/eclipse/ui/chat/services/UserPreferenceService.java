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
import com.microsoft.copilot.eclipse.ui.chat.ChatView;
import com.microsoft.copilot.eclipse.ui.chat.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.CustomModesPreferencePage;
import com.microsoft.copilot.eclipse.ui.swt.DropdownButton;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItem;
import com.microsoft.copilot.eclipse.ui.swt.DropdownItemGroup;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Service for managing chat modes and input navigation.
 */
public class UserPreferenceService extends ChatBaseService implements CopilotAuthStatusListener {
  private IObservableValue<String[]> chatModeObservable;
  private IObservableValue<ChatMode> activeChatModeObservable; // Controls which view to show: Ask or Agent
  private IObservableValue<String> activeModeNameOrIdObservable; // Tracks current mode name/ID for UI elements
  private InputNavigation inputNavigation = new InputNavigation();

  // Track side effects for chat mode button only
  private final Map<DropdownButton, ISideEffect[]> chatModeButtonSideEffects = new HashMap<>();
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
      chatModeObservable = new WritableValue<>(getAvailableChatModes(), String[].class);
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
              if (!Arrays.deepEquals(getAvailableChatModes(), chatModeObservable.getValue())) {
                chatModeObservable.setValue(getAvailableChatModes());
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
          if (!Arrays.deepEquals(getAvailableChatModes(), chatModeObservable.getValue())) {
            chatModeObservable.setValue(getAvailableChatModes());
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
  private String[] getAvailableChatModes() {
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

    // Add custom modes
    List<CustomChatMode> customModes = CustomChatModeManager.INSTANCE.getCustomModes();
    if (!customModes.isEmpty()) {
      customModes.forEach(mode -> modes.add(mode.getDisplayName()));
    }

    return modes.toArray(new String[0]);
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
        String[] updatedModes = getAvailableChatModes();

        // Only update if the modes have changed
        if (!Arrays.deepEquals(currentModes, updatedModes)) {
          chatModeObservable.setValue(updatedModes);
        }
      });
    });
  }

  /**
   * Binds a {@link DropdownButton} to this service for chat mode selection. The button displays
   * built-in and custom modes separated into groups. Clicking "Configure Modes" opens the
   * preference page without changing the active selection.
   *
   * @param picker the dropdown button to bind
   */
  public void bindChatModePicker(final DropdownButton picker) {
    unbindChatModePicker(picker);

    Runnable configureModes = () -> {
      PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
          SwtUtils.getDisplay().getActiveShell(), CustomModesPreferencePage.ID,
          PreferencesUtils.getAllPreferenceIds(), null);
      dialog.open();
      CustomChatModeManager.INSTANCE.syncCustomModesFromService().thenAccept(v -> {
        ensureRealm(() -> chatModeObservable.setValue(getAvailableChatModes()));
        String current = restoreChatModeName();
        if (isModeAvailable(current)) {
          // Mode is still available; explicitly refresh the picker so its displayed
          // label stays in sync after the group list was rebuilt.
          SwtUtils.invokeOnDisplayThreadAsync(() -> {
            if (!picker.isDisposed()) {
              picker.setSelectedItemId(current);
            }
          }, picker);
        } else {
          setActiveChatMode(ChatMode.Agent.toString());
        }
      });
    };

    ensureRealm(() -> {
      ISideEffect groupsSideEffect = ISideEffect.create(() -> {
        return buildChatModeGroups(chatModeObservable.getValue(), configureModes);
      }, (List<DropdownItemGroup> groups) -> {
        if (!picker.isDisposed()) {
          picker.setItemGroups(groups);
        }
      });

      ISideEffect selectionSideEffect = ISideEffect.create(() -> {
        return activeModeNameOrIdObservable.getValue();
      }, (String modeNameOrId) -> {
        if (!picker.isDisposed() && modeNameOrId != null) {
          picker.setSelectedItemId(modeNameOrId);
        }
      });

      chatModeButtonSideEffects.put(picker, new ISideEffect[] { groupsSideEffect, selectionSideEffect });
      // Add a dispose listener to auto-unbind when the dropdown button is disposed
      picker.addDisposeListener(e -> unbindChatModePicker(picker));
    });
  }

  /**
   * Unbind and dispose side effects for a specific DropdownButton chat mode picker.
   *
   * @param picker the dropdown button to unbind
   */
  public void unbindChatModePicker(DropdownButton picker) {
    ISideEffect[] effects = chatModeButtonSideEffects.remove(picker);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  private List<DropdownItemGroup> buildChatModeGroups(String[] availableModeNames, Runnable configureModes) {
    List<String> available = Arrays.asList(availableModeNames);

    List<DropdownItem> builtInItems = new ArrayList<>();
    for (BuiltInChatMode mode : BuiltInChatModeManager.INSTANCE.getBuiltInModes()) {
      if (!available.contains(mode.getDisplayName())) {
        continue;
      }
      builtInItems.add(new DropdownItem.Builder()
          .id(mode.getDisplayName())
          .label(mode.getDisplayName())
          .tooltip(mode.getDescription())
          .build());
    }

    List<DropdownItem> customItems = new ArrayList<>();
    for (CustomChatMode mode : CustomChatModeManager.INSTANCE.getCustomModes()) {
      if (!available.contains(mode.getDisplayName())) {
        continue;
      }
      customItems.add(new DropdownItem.Builder()
          .id(mode.getId())
          .label(mode.getDisplayName())
          .tooltip(mode.getDescription())
          .build());
    }

    List<DropdownItemGroup> groups = new ArrayList<>();
    if (!builtInItems.isEmpty()) {
      groups.add(DropdownItemGroup.of(builtInItems));
    }
    if (!customItems.isEmpty()) {
      groups.add(DropdownItemGroup.of(customItems));
    }

    // Add "Configure Modes" option only if custom agent feature is enabled
    if (FeatureFlags.isCustomAgentEnabled()) {
      List<DropdownItem> configItems = new ArrayList<>();
      configItems.add(new DropdownItem.Builder()
          .label(Messages.configureModes)
          .onAction(configureModes)
          .build());
      groups.add(DropdownItemGroup.of(configItems));
    }
    return groups;
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

  private boolean isModeAvailable(String modeNameOrId) {
    String[] available = getAvailableChatModes();
    // Check built-in modes (matched by display name)
    if (Arrays.asList(available).contains(modeNameOrId)) {
      return true;
    }
    // Check custom modes (matched by ID, displayed by name)
    if (CustomChatModeManager.INSTANCE.isCustomMode(modeNameOrId)) {
      CustomChatMode mode = CustomChatModeManager.INSTANCE.getCustomModeById(modeNameOrId);
      return mode != null && Arrays.asList(available).contains(mode.getDisplayName());
    }
    return false;
  }

  private void disposeAllSideEffects() {
    ensureRealm(() -> {
      // Dispose chat mode combo side effects
      for (ISideEffect[] effects : chatModeButtonSideEffects.values()) {
        for (ISideEffect effect : effects) {
          if (effect != null) {
            effect.dispose();
          }
        }
      }
    });

    chatModeButtonSideEffects.clear();
  }
}
