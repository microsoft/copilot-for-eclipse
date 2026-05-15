// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget to display a message when the user has no quota.
 */
public class WarnWidget extends Composite {
  private int buttonLeftMargin;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param message the message to display
   */
  public WarnWidget(Composite parent, int style, String message, int code) {
    super(parent, style | SWT.BORDER);
    setLayout(new GridLayout(1, true));
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    buildWarnLabelWithIcon(message);

    // 402 = quota exceeded. The server bakes the recommended next steps into the message text itself
    // (see copilot-language-server-internal fetch.ts), so we drive button visibility off of message
    // content to keep parity with the IntelliJ UpgradeNotificationComponent#initTbb rendering. See:
    // https://github.com/microsoft/copilot-client/blob/77f8f28e1a1e2efb51b6f92649bd9d085b8b64f5/lib/src/conversation/fetchPostProcessor.ts#L232-L248
    if (code == 402) {
      buildActionButtonsFromMessage(message);
    }
    parent.layout();
  }

  private void buildWarnLabelWithIcon(String message) {
    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

    Label iconLabel = new Label(composite, SWT.TOP);
    Image warnImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    iconLabel.setImage(warnImage);
    GridData iconGd = new GridData(SWT.LEFT, SWT.TOP, false, false);
    iconGd.verticalIndent = 4;
    iconLabel.setLayoutData(iconGd);
    buttonLeftMargin = warnImage.getBounds().width + iconGd.verticalIndent;

    ChatMarkupViewer textLabel = new ChatMarkupViewer(composite, SWT.LEFT | SWT.WRAP);
    StyledText styledText = textLabel.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
    styledText.setEditable(false);
    textLabel.setMarkup(message);

    requestLayout();
  }

  /**
   * Render action buttons based on phrases present in the 402 message body, mirroring the IntelliJ
   * {@code UpgradeNotificationComponent#initTbb} logic:
   * <ul>
   *   <li>{@code "additional overage"} or {@code "additional usage"} &rarr; "Enable Additional Usage"
   *       (manage-overage URL)</li>
   *   <li>{@code "increase budget"} (when neither overage nor usage phrase is present) &rarr;
   *       "Increase Budget" (manage-overage URL)</li>
   *   <li>{@code "upgrade your plan"} or the legacy {@code "30-day free trial"} hint &rarr;
   *       "Upgrade Plan" (upgrade-plan URL)</li>
   * </ul>
   *
   * <p>The overage button is shown as primary when present; the upgrade button is primary only when no
   * overage button is rendered, matching the IntelliJ button styling.
   */
  private void buildActionButtonsFromMessage(String message) {
    if (message == null) {
      return;
    }
    String lower = message.toLowerCase(Locale.ROOT);
    boolean enableAdditionalUsage = lower.contains("additional overage") || lower.contains("additional usage");
    boolean increaseBudget = !enableAdditionalUsage && lower.contains("increase budget");
    boolean upgradePlan = lower.contains("upgrade your plan") || lower.contains("30-day free trial");
    if (!enableAdditionalUsage && !increaseBudget && !upgradePlan) {
      return;
    }

    Composite composite = new Composite(this, SWT.NONE);
    RowLayout layout = new RowLayout(SWT.HORIZONTAL);
    layout.marginLeft = this.buttonLeftMargin; // Align with the message text
    layout.spacing = 10;
    composite.setLayout(layout);

    boolean overageButtonShown = enableAdditionalUsage || increaseBudget;
    if (enableAdditionalUsage) {
      addActionButton(composite, Messages.menu_quota_enableAdditionalUsage,
          UiConstants.MANAGE_COPILOT_OVERAGE_URL, true);
    } else if (increaseBudget) {
      addActionButton(composite, Messages.menu_quota_increaseBudget,
          UiConstants.MANAGE_COPILOT_OVERAGE_URL, true);
    }
    if (upgradePlan) {
      addActionButton(composite, Messages.menu_quota_upgradePlan,
          UiConstants.COPILOT_UPGRADE_PLAN_URL, !overageButtonShown);
    }
  }

  private static void addActionButton(Composite parent, String label, String link, boolean primary) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    button.setToolTipText(label);
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
        UiUtils.openLink(link);
      }
    });
    if (primary) {
      button.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
    }
  }
}
