// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage;
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.State;

/**
 * Inline preference-loading status, independent of model and conversation loading.
 */
public class PreferenceStatus extends Composite {
  /**
   * Creates a status message and an explicit retry action.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(2, false));
    GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
    setLayoutData(data);
    Label message = new Label(this, SWT.WRAP);
    message.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    message.setData("org.eclipse.swtbot.widget.key", "preference-status");
    Link retry = new Link(this, SWT.NONE);
    retry.setText("<a>" + Messages.preferenceRetry + "</a>");
    retry.setData("org.eclipse.swtbot.widget.key", "preference-retry");
    GridData retryData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    retry.setLayoutData(retryData);
    retry.addListener(SWT.Selection, event -> storage.retry());
    Realm.runWithDefault(storage.getReadiness().getRealm(), () -> {
      ISideEffect effect = ISideEffect.create(storage.getReadiness()::getValue, state -> {
        if (isDisposed()) {
          return;
        }
        boolean visible = state != State.READY && state != State.DISPOSED;
        data.exclude = !visible;
        setVisible(visible);
        message.setText(switch (state) {
          case LOADING -> Messages.preferenceLoading;
          case FAILED -> Messages.preferenceLoadFailed;
          default -> Messages.preferenceUnavailable;
        });
        boolean canRetry = state == State.FAILED || state == State.UNAVAILABLE;
        retryData.exclude = !canRetry;
        retry.setVisible(canRetry);
        retry.setEnabled(canRetry);
        requestLayout();
      });
      addDisposeListener(event -> effect.dispose());
    });
  }
}
