package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotAuthStatusListener;
import com.microsoft.copilot.eclipse.core.chat.UserPreference;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Service for managing Copilot chat modes.
 */
public class ChatModeService extends ChatBaseService implements CopilotAuthStatusListener {
  // Data
  private IObservableValue<ChatMode> activeChatModeObservable;

  // Track side effects for each combo
  private final Map<Combo, ISideEffect[]> comboSideEffects = new HashMap<>();

  /**
   * Constructor for the ChatModeService.
   */
  public ChatModeService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    super(lsConnection, authStatusManager);

    this.authStatusManager.addCopilotAuthStatusListener(this);
    ensureRealm(() -> {
      activeChatModeObservable = new WritableValue<>(null, ChatMode.class);
    });
  }

  /**
   * Set the active chat mode.
   *
   * @param chatModeName the name of the chat mode
   */
  public void setActiveChatMode(String chatModeName) {
    // Persist the chat mode selection
    UserPreference preference = getUserPreference();
    preference.setChatModeName(chatModeName);
    persistUserPreference();

    ensureRealm(() -> {
      activeChatModeObservable.setValue(ChatMode.valueOf(chatModeName));
    });
  }

  /**
   * Get the active chat mode.
   *
   * @return the active chat mode
   */
  public String getActiveChatMode() {
    ChatMode activeChatMode = activeChatModeObservable.getValue();
    return activeChatMode == null ? ChatMode.Ask.toString() : activeChatMode.toString();
  }

  @Override
  public void onDidCopilotStatusChange(CopilotStatusResult copilotStatusResult) {
    String status = copilotStatusResult.getStatus();
    switch (status) {
      case CopilotStatusResult.OK:
        String chatModeName = restoreChatModeName();
        ensureRealm(() -> {
          activeChatModeObservable.setValue(ChatMode.valueOf(chatModeName));
        });
        break;
      default:
        ensureRealm(() -> {
          activeChatModeObservable.setValue(null);
        });
        break;
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
      ISideEffect activeChatModeSideEffect = ISideEffect.create(() -> {
        ChatMode activeMode = this.activeChatModeObservable.getValue();
        return activeMode == null ? ChatMode.Ask.toString() : activeMode.toString();
      }, (String modeName) -> {
        if (combo.isDisposed()) {
          return;
        }
        int index = Arrays.asList(combo.getItems()).indexOf(modeName);
        if (index >= 0) {
          combo.select(index);
          // Adjust the width according to the item
          GC gc = new GC(combo);
          Point textExtent = gc.textExtent(modeName);
          gc.dispose();

          GridData gridData = (GridData) combo.getLayoutData();
          // Add some padding
          int padding = PlatformUtils.isWindows() ? 0 : 40;
          gridData.widthHint = textExtent.x + padding;

          combo.getParent().getParent().layout();
        }
      });

      // Store the side effects for later disposal
      comboSideEffects.put(combo, new ISideEffect[] { activeChatModeSideEffect });

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
    ISideEffect[] effects = comboSideEffects.remove(combo);
    if (effects != null) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }
  }

  /**
   * Dispose the service.
   */
  public void dispose() {
    // Dispose all combo side effects
    for (ISideEffect[] effects : comboSideEffects.values()) {
      for (ISideEffect effect : effects) {
        if (effect != null) {
          effect.dispose();
        }
      }
    }

    comboSideEffects.clear();

    if (activeChatModeObservable != null) {
      activeChatModeObservable.dispose();
      activeChatModeObservable = null;
    }

    this.authStatusManager.removeCopilotAuthStatusListener(this);
  }

  private String restoreChatModeName() {
    UserPreference preference = getUserPreference();
    if (preference != null && preference.getChatModeName() != null) {
      return preference.getChatModeName();
    }

    return ChatMode.Ask.toString();
  }
}
