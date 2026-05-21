// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.chat.services.AvatarService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.swt.SpinnerAnimator;
import com.microsoft.copilot.eclipse.ui.utils.AccessibilityUtils;
import com.microsoft.copilot.eclipse.ui.utils.ModelUtils;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * A custom widget that displays a turn for the copilot.
 */
public class CopilotTurnWidget extends ThinkingTurnWidget {

  private Composite compactingComposite;
  private SpinnerAnimator compactingSpinner;

  /**
   * Create the widget.
   */
  public CopilotTurnWidget(Composite parent, int style, ChatServiceManager serviceManager, String turnId) {
    super(parent, style, serviceManager, turnId, null);
    setData("org.eclipse.swtbot.widget.key", "copilot-turn");
  }

  @Override
  protected Image getAvatar(AvatarService avatarService) {
    return avatarService.getAvatarForCopilot();
  }

  @Override
  protected String getRoleName() {
    return Messages.chat_turnWidget_copilot;
  }

  @Override
  protected Label createAvatarLabel(Composite parent) {
    return new Label(parent, SWT.NONE);
  }

  @Override
  protected void createTextBlock() {
    this.currentTextBlock = new ChatMarkupViewer(this, SWT.MULTI | SWT.WRAP);
    StyledText styledText = this.currentTextBlock.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
    styledText.setEditable(false);

    AccessibilityUtils.addFocusBorderToComposite(styledText);
  }

  /**
   * Render the model information for this Copilot turn. This method should be called after receiving ChatTurnResult or
   * ChatCreateResult.
   *
   * @param modelName the name of the model used
   * @param billingMultiplier the billing multiplier for the model
   * @param reasoningEffort the reasoning effort that was sent for this turn (may be {@code null} or blank if the model
   *     does not support reasoning effort or the user did not select one)
   */
  public void renderModelInfo(String modelName, double billingMultiplier, String reasoningEffort) {
    if (modelName != null && !modelName.isEmpty()) {
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        if (footer == null || footer.isDisposed()) {
          createFooter();
        }
        if (StringUtils.isNotBlank(modelName)) {
          Label modelInfoLabel = new Label(footer, SWT.NONE);
          String formattedEffort = ModelUtils.formatReasoningEffortLevel(reasoningEffort);
          String modelWithEffort;
          if (StringUtils.isNotBlank(formattedEffort)) {
            modelWithEffort = modelName + " - " + formattedEffort;
          } else {
            modelWithEffort = modelName;
          }
          // When token-based billing is enabled on the language server, the per-turn billing
          // multiplier is no longer a meaningful price signal, so render the model name on its
          // own. Fall back to the legacy "{model} - {multiplier}" format otherwise.
          boolean tbbEnabled = CopilotCore.getPlugin().getAuthStatusManager()
              .getQuotaStatus().tokenBasedBillingEnabled();
          String displayText;
          if (tbbEnabled) {
            displayText = modelWithEffort;
          } else {
            // TODO: Remove this legacy fallback after TBB is officially released.
            String formattedMultiplier = ModelUtils.formatBillingMultiplier(billingMultiplier);
            displayText = String.format("%s - %s", modelWithEffort, formattedMultiplier);
          }
          modelInfoLabel.setText(displayText);
          GridData labelGridData = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
          modelInfoLabel.setLayoutData(labelGridData);
          modelInfoLabel.setData(CssConstants.CSS_CLASS_NAME_KEY, "model-info-label");

          footer.requestLayout();
        }
      }, this);
    }
  }

  /**
   * Shows a "Compacting conversation..." spinner below the last message in this turn.
   * Must be called on the UI thread.
   */
  public void showCompactingStatus() {
    if (isDisposed() || compactingComposite != null) {
      return;
    }
    compactingComposite = new Composite(this, SWT.NONE);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 4;
    compactingComposite.setLayout(layout);
    compactingComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label spinnerLabel = new Label(compactingComposite, SWT.NONE);
    spinnerLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    compactingSpinner = new SpinnerAnimator(spinnerLabel);
    compactingSpinner.start();

    Label statusLabel = new Label(compactingComposite, SWT.NONE);
    statusLabel.setText(Messages.chat_compacting_conversation);
    statusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

    ensureFooterAtBottom();
    requestLayout();
  }

  /**
   * Hides the "Compacting conversation..." spinner.
   * Must be called on the UI thread.
   */
  public void hideCompactingStatus() {
    if (compactingSpinner != null) {
      compactingSpinner.stop();
      compactingSpinner = null;
    }
    if (compactingComposite != null && !compactingComposite.isDisposed()) {
      compactingComposite.dispose();
      compactingComposite = null;
    }
    requestLayout();
  }

  @Override
  protected void createFooter() {
    footer = new Composite(this, SWT.NONE);
    footer.setLayout(new GridLayout(1, false));
    footer.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
  }

}
