// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.swt.widgets.Display;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Base class for chat services.
 */
public abstract class ChatBaseService {
  protected CopilotLanguageServerConnection lsConnection;
  protected AuthStatusManager authStatusManager;

  /**
   * Constructor for the ChatBaseService.
   */
  protected ChatBaseService(CopilotLanguageServerConnection lsConnection, AuthStatusManager authStatusManager) {
    this.lsConnection = lsConnection;
    this.authStatusManager = authStatusManager;
  }

  /**
   * Ensures operations run in the correct Realm.
   *
   * @param runnable The code to execute in the UI Realm.
   */
  protected void ensureRealm(Runnable runnable) {
    // If we're already in the UI thread
    if (Display.getCurrent() != null) {
      Realm realm = Realm.getDefault();
      if (realm == null) {
        realm = DisplayRealm.getRealm(Display.getCurrent());
      }
      Realm.runWithDefault(realm, runnable::run);
    } else {
      SwtUtils.invokeOnDisplayThread(() -> {
        Realm realm = DisplayRealm.getRealm(Display.getDefault());
        Realm.runWithDefault(realm, runnable::run);
      });
    }
  }

  /**
   * Update the value of an observable in its realm.
   *
   * @param observable The observable to update.
   * @param value The new value to set.
   */
  protected <T> void updateObservable(IObservableValue<T> observable, final T value) {
    if (observable != null) {
      observable.getRealm().asyncExec(() -> {
        if (!observable.isDisposed()) {
          observable.setValue(value);
        }
      });
    }
  }
}
