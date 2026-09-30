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
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService.ModeDiscoveryState;

/**
 * Inline preference-loading status, independent of model and conversation loading.
 */
public class PreferenceStatus extends Composite {
  /**
   * Creates a status message and an explicit recovery action.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage) {
    this(parent, storage, null);
  }

  /**
   * Creates status and recovery/retry controls for preferences and independent mode discovery.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   * @param preferences mode-discovery owner, or {@code null} for preference-only status
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage, UserPreferenceService preferences) {
    super(parent, SWT.NONE);
    setLayout(new GridLayout(3, false));
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
      State state = storage.getState();
      if (preferences != null && preferences.getModeDiscoveryState() != ModeDiscoveryState.READY
          && preferences.getModeDiscoveryState() != ModeDiscoveryState.LOADING) {
        preferences.retryModeDiscovery();
      }
      if (state == State.FAILED || state == State.UNAVAILABLE) {
        storage.retry();
      }
    });
    Link restore = new Link(this, SWT.NONE);
    restore.setText("<a>" + Messages.preferenceRestoreDefaults + "</a>");
    restore.setData("org.eclipse.swtbot.widget.key", "preference-restore-defaults");
    GridData restoreData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    restore.setLayoutData(restoreData);
    restore.addListener(SWT.Selection, event -> storage.restoreDefaults());
    Realm.runWithDefault(storage.getReadiness().getRealm(), () -> {
      ISideEffect effect = ISideEffect.create(() -> {
        return new Readiness(storage.getReadiness().getValue(),
            preferences == null ? ModeDiscoveryState.READY : preferences.getModeDiscoveryState());
      }, readiness -> {
        if (isDisposed()) {
          return;
        }
        State state = readiness.preferences();
        boolean modePending = state == State.READY && readiness.modes() != ModeDiscoveryState.READY;
        boolean visible = state != State.DISPOSED && (state != State.READY || modePending);
        data.exclude = !visible;
        setVisible(visible);
        String text = switch (state) {
          case LOADING -> Messages.preferenceLoading;
          case FAILED -> Messages.preferenceLoadFailed;
          case CORRUPT -> Messages.preferenceCorrupt;
          case RESTORING -> Messages.preferenceRestoringDefaults;
          case RESTORE_FAILED -> Messages.preferenceRestoreFailed;
          default -> Messages.preferenceUnavailable;
        };
        if (modePending) {
          text = readiness.modes() == ModeDiscoveryState.LOADING
              ? Messages.modeDiscoveryLoading : Messages.modeDiscoveryFailed;
        }
        message.setText(text);
        boolean canRetry = (state == State.FAILED || state == State.UNAVAILABLE)
            || (modePending && readiness.modes() != ModeDiscoveryState.LOADING)
            || (state != State.READY && preferences != null
                && (readiness.modes() == ModeDiscoveryState.FAILED
                    || readiness.modes() == ModeDiscoveryState.UNAVAILABLE));
        final boolean canRestore = state == State.CORRUPT || state == State.RESTORING || state == State.RESTORE_FAILED;
        retryData.exclude = !canRetry;
        retry.setVisible(canRetry);
        retry.setEnabled(canRetry);
        restoreData.exclude = !canRestore;
        restore.setVisible(canRestore);
        restore.setEnabled(state != State.RESTORING);
        requestLayout();
      });
      addDisposeListener(event -> effect.dispose());
    });
  }

  private record Readiness(State preferences, ModeDiscoveryState modes) {
  }
}
