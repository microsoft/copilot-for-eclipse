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
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.SaveState;
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.State;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService.ModeDiscoveryState;

/**
 * Inline preference loading and unsaved status, independent of model and conversation loading.
 */
public class PreferenceStatus extends Composite {
  /**
   * Creates a status message and an explicit retry action.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage) {
    this(parent, storage, null);
  }

  /**
   * Creates status and retry controls for preference loading and independent mode discovery.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   * @param preferences mode-discovery owner, or {@code null} for preference-only status
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage, UserPreferenceService preferences) {
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
    retry.addListener(SWT.Selection, event -> {
      if (storage.getState() == State.READY) {
        if (storage.getSaveStatus().getValue() == SaveState.FAILED) {
          storage.persist();
        } else if (preferences != null) {
          preferences.retryModeDiscovery();
        }
      } else {
        storage.retry();
      }
    });
    Realm.runWithDefault(storage.getReadiness().getRealm(), () -> {
      ISideEffect effect = ISideEffect.create(() -> {
        return new Readiness(storage.getReadiness().getValue(),
            preferences == null ? ModeDiscoveryState.READY : preferences.getModeDiscoveryState(),
            storage.getSaveStatus().getValue());
      }, readiness -> {
        if (isDisposed()) {
          return;
        }
        State state = readiness.preferences();
        boolean modePending = state == State.READY && readiness.modes() != ModeDiscoveryState.READY;
        boolean unsaved = state == State.READY && readiness.save() == SaveState.FAILED;
        boolean visible = state != State.DISPOSED && (state != State.READY || modePending || unsaved);
        data.exclude = !visible;
        setVisible(visible);
        String text = switch (state) {
          case LOADING -> Messages.preferenceLoading;
          case FAILED -> Messages.preferenceLoadFailed;
          default -> Messages.preferenceUnavailable;
        };
        if (modePending) {
          text = readiness.modes() == ModeDiscoveryState.LOADING
              ? Messages.modeDiscoveryLoading : Messages.modeDiscoveryFailed;
        }
        if (unsaved) {
          text = Messages.preferenceSaveFailed;
        }
        message.setText(text);
        boolean canRetry = unsaved || state == State.FAILED || state == State.UNAVAILABLE
            || (modePending && readiness.modes() != ModeDiscoveryState.LOADING);
        retryData.exclude = !canRetry;
        retry.setVisible(canRetry);
        retry.setEnabled(canRetry);
        requestLayout();
      });
      addDisposeListener(event -> effect.dispose());
    });
  }

  private record Readiness(State preferences, ModeDiscoveryState modes, SaveState save) {
  }
}
