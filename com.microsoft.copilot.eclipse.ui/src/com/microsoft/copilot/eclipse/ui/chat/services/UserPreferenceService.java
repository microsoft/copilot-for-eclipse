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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.EventHandler;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.InputNavigation;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.events.CopilotEventConstants;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Service for managing chat modes and input navigation.
 */
public class UserPreferenceService extends ChatBaseService implements CopilotAuthStatusListener {
  private static final int EXTRA_PADDING = 40;
  private IObservableValue<String[]> chatModeObservable;
  private IObservableValue<ChatMode> activeChatModeObservable;
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
      ChatMode chatMode = ChatMode.valueOf(chatModeName);
      FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
      if (flags != null && !flags.isAgentModeEnabled()) {
        chatMode = ChatMode.Ask; // Only Ask mode is available
      }
      final ChatMode finalChatMode = chatMode;
      ensureRealm(() -> activeChatModeObservable.setValue(finalChatMode));
      inputNavigation = new InputNavigation(restoreUserInputs());
    }
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
   * Get available chat modes based on feature flags.
   */
  private String[] getAvalibleChatModes() {
    FeatureFlags flags = CopilotCore.getPlugin().getFeatureFlags();
    if (flags != null && !flags.isAgentModeEnabled()) {
      return new String[] { ChatMode.Ask.displayName() }; // Only Ask mode is available
    }

    return Arrays.stream(ChatMode.values()).map(ChatMode::displayName).toArray(String[]::new);
  }

  /**
   * Set the active chat mode by index.
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
   * Set the active chat mode by name.
   *
   * @param chatModeName the name of the chat mode to set
   */
  public void setActiveChatMode(String chatModeName) {
    if (StringUtils.isBlank(chatModeName) || chatModeName.equals(getActiveChatMode().toString())) {
      return;
    }

    // Persist the chat mode selection
    UserPreference preference = getUserPreference();
    preference.setChatModeName(chatModeName);
    persistUserPreference();

    ensureRealm(() -> activeChatModeObservable.setValue(ChatMode.valueOf(chatModeName)));

    // Publish chat mode change event for other services (like ModelService)
    if (eventBroker != null) {
      eventBroker.post(CopilotEventConstants.TOPIC_CHAT_MODE_CHANGED, ChatMode.valueOf(chatModeName));
    }
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
          }
        }
      });

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
    if (inputNavigation == null) {
      return;
    }

    inputNavigation.updateCursorPosition(inputNavigation.size());
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
