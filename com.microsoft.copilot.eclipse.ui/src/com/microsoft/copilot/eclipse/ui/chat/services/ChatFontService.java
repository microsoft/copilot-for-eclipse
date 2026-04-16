// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Service for managing chat font changes across all chat UI controls.
 *
 * <p>This service centralizes font change event handling, maintaining a single listener to the theme's FontRegistry and
 * propagating font updates to all registered controls.
 */
public class ChatFontService {

  private final Set<Control> registeredControls = ConcurrentHashMap.newKeySet();
  private final Set<Runnable> fontChangeCallbacks = ConcurrentHashMap.newKeySet();
  private IPropertyChangeListener fontChangeListener;

  /**
   * Creates a new ChatFontService and registers a listener for font changes.
   */
  public ChatFontService() {
    fontChangeListener = event -> {
      if (UiUtils.CHAT_FONT_ID.equals(event.getProperty())) {
        applyFontToAllControls();
      }
    };

    FontRegistry fontRegistry = UiUtils.getThemeFontRegistry();
    if (fontRegistry != null) {
      fontRegistry.addListener(fontChangeListener);
    }
  }

  /**
   * Registers a control to receive chat font updates.
   *
   * <p>The control will have the chat font applied immediately and will be automatically updated when the chat font
   * changes. The control is automatically unregistered when it is disposed.
   *
   * @param control the control to register for font updates
   */
  public void registerControl(Control control) {
    if (control == null || control.isDisposed()) {
      return;
    }

    // Add returns false if already present (idempotent)
    if (!registeredControls.add(control)) {
      return;
    }

    // Apply font immediately
    UiUtils.applyChatFont(control);

    // Auto-remove on dispose
    control.addDisposeListener(e -> registeredControls.remove(control));
  }

  /**
   * Unregisters a control from receiving font updates.
   *
   * <p>This is typically not needed as controls are automatically unregistered when disposed, but can be used for
   * explicit cleanup.
   *
   * @param control the control to unregister
   */
  public void unregisterControl(Control control) {
    if (control != null) {
      registeredControls.remove(control);
    }
  }

  /**
   * Registers a callback to be invoked when the chat font changes.
   *
   * <p>This is useful for controls that need custom font handling, such as creating derived fonts (e.g., bold
   * variants). The callback will be invoked immediately and whenever the chat font changes.
   *
   * @param callback the callback to invoke on font changes
   */
  public void registerCallback(Runnable callback) {
    if (callback == null) {
      return;
    }

    // Add returns false if already present (idempotent)
    if (!fontChangeCallbacks.add(callback)) {
      return;
    }

    try {
      callback.run();
      fontChangeCallbacks.add(callback);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to execute font change callback", e);
    }
  }

  /**
   * Unregisters a font change callback.
   *
   * @param callback the callback to unregister
   */
  public void unregisterCallback(Runnable callback) {
    if (callback != null) {
      fontChangeCallbacks.remove(callback);
    }
  }

  /**
   * Applies the current chat font to all registered controls and invokes all callbacks.
   */
  private void applyFontToAllControls() {
    Display display = Display.getDefault();
    if (display == null || display.isDisposed()) {
      return;
    }

    display.asyncExec(() -> {
      for (Control control : registeredControls) {
        if (control != null && !control.isDisposed()) {
          UiUtils.applyChatFont(control);
          control.requestLayout();
        }
      }

      for (Runnable callback : fontChangeCallbacks) {
        try {
          callback.run();
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Failed to execute font change callback", e);
        }
      }
    });
  }

  /**
   * Disposes of the font service and releases all resources.
   */
  public void dispose() {
    if (fontChangeListener != null) {
      FontRegistry fontRegistry = UiUtils.getThemeFontRegistry();
      if (fontRegistry != null) {
        fontRegistry.removeListener(fontChangeListener);
      }
      fontChangeListener = null;
    }

    registeredControls.clear();
    fontChangeCallbacks.clear();
  }
}