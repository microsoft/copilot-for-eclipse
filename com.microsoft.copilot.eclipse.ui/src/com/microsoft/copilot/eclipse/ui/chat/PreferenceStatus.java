// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.sideeffect.ISideEffect;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage;
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.InitializationResult;
import com.microsoft.copilot.eclipse.ui.chat.services.PreferenceStorage.State;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService.ModeDiscoveryState;

/**
 * Inline preference initialization results and independent mode-discovery status.
 */
public class PreferenceStatus extends Composite {
  /**
   * Creates a preference result message.
   *
   * @param parent parent control
   * @param storage shared chat preference storage
   */
  public PreferenceStatus(Composite parent, PreferenceStorage storage) {
    this(parent, storage, null);
  }

  /**
   * Creates result and retry controls for preference initialization and independent mode discovery.
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
    retry.setText("<a>" + Messages.modeDiscoveryRetry + "</a>");
    retry.setData("org.eclipse.swtbot.widget.key", "mode-discovery-retry");
    retry.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    retry.addListener(SWT.Selection, event -> {
      if (preferences != null) {
        preferences.retryModeDiscovery();
      }
    });
    Button dismiss = new Button(this, SWT.PUSH);
    dismiss.setText(Messages.preferenceDismiss);
    dismiss.setData("org.eclipse.swtbot.widget.key", "preference-dismiss");
    dismiss.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    dismiss.addListener(SWT.Selection, event -> storage.dismissInitializationResult());
    Realm.runWithDefault(storage.getReadiness().getRealm(), () -> {
      ISideEffect effect = ISideEffect.create(() -> {
        return new Readiness(storage.getReadiness().getValue(),
            storage.getInitializationResult().getValue(),
            preferences == null ? ModeDiscoveryState.READY : preferences.getModeDiscoveryState());
      }, readiness -> {
        if (isDisposed()) {
          return;
        }
        State state = readiness.preferences();
        boolean modePending = state == State.READY && readiness.modes() != ModeDiscoveryState.READY;
        boolean preferenceResult = (state == State.READY && readiness.result() == InitializationResult.RECOVERED)
            || (state == State.FAILED && readiness.result() == InitializationResult.FAILED);
        boolean visible = state != State.DISPOSED && (preferenceResult || modePending);
        data.exclude = !visible;
        setVisible(visible);
        String text = preferenceResult
            ? readiness.result() == InitializationResult.RECOVERED
                ? Messages.preferenceRecovered : Messages.preferenceRecoveryFailed
            : "";
        if (modePending) {
          text = readiness.modes() == ModeDiscoveryState.LOADING
              ? Messages.modeDiscoveryLoading : Messages.modeDiscoveryFailed;
        }
        message.setText(text);
        boolean canRetryModes = modePending && readiness.modes() == ModeDiscoveryState.FAILED;
        retry.setVisible(canRetryModes);
        retry.setEnabled(canRetryModes);
        boolean canDismiss = preferenceResult && !modePending;
        dismiss.setVisible(canDismiss);
        dismiss.setEnabled(canDismiss);
        ((GridData) retry.getLayoutData()).exclude = !canRetryModes;
        ((GridData) dismiss.getLayoutData()).exclude = !canDismiss;
        requestLayout();
      });
      addDisposeListener(event -> effect.dispose());
    });
  }

  private record Readiness(State preferences, InitializationResult result, ModeDiscoveryState modes) {
  }
}
