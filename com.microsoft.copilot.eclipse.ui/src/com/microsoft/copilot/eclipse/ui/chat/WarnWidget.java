// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

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

import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.ui.chat.QuotaActions.QuotaAction;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Widget that displays a warning message under a chat turn, optionally followed by plan-driven action buttons sourced
 * from {@link QuotaActions#forPlan(CopilotPlan, boolean)}. Presentation-only: the caller decides the message and
 * whether to pass a plan.
 */
public class WarnWidget extends Composite {
  private int buttonLeftMargin;

  /**
   * Create the composite.
   *
   * @param parent the parent composite
   * @param style the SWT style bits
   * @param message the message to display ({@code null} treated as empty)
   * @param userPlan the user's Copilot plan to render plan-driven action buttons, or {@code null} for no buttons
   * @param overageEnabled whether additional paid usage is already enabled for the user; switches the
   *     "Enable Additional Usage" label to "Increase Budget"
   */
  public WarnWidget(Composite parent, int style, String message, CopilotPlan userPlan, boolean overageEnabled) {
    super(parent, style | SWT.BORDER);
    GridLayout outerLayout = new GridLayout(1, true);
    outerLayout.verticalSpacing = 0;
    setLayout(outerLayout);
    setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    buildWarnLabelWithIcon(message);

    if (userPlan != null) {
      buildActionButtons(userPlan, overageEnabled);
    }
    parent.layout();
  }

  private void buildWarnLabelWithIcon(String message) {
    Composite composite = new Composite(this, SWT.NONE);
    GridLayout warnLayout = new GridLayout(2, false);
    composite.setLayout(warnLayout);
    composite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

    Label iconLabel = new Label(composite, SWT.TOP);
    Image warnImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
    iconLabel.setImage(warnImage);
    GridData iconGd = new GridData(SWT.LEFT, SWT.TOP, false, false);
    iconGd.verticalIndent = 4;
    iconLabel.setLayoutData(iconGd);
    buttonLeftMargin = warnLayout.marginWidth + warnLayout.marginLeft + warnImage.getBounds().width
        + warnLayout.horizontalSpacing;

    ChatMarkupViewer textLabel = new ChatMarkupViewer(composite, SWT.LEFT | SWT.WRAP);
    StyledText styledText = textLabel.getTextWidget();
    styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
    styledText.setEditable(false);
    textLabel.setMarkup(message);

    requestLayout();
  }

  /**
   * Render plan-driven action buttons for a quota-exceeded warning, kept in sync with the quota {@link StaticBanner}.
   */
  private void buildActionButtons(CopilotPlan userPlan, boolean overageEnabled) {
    List<QuotaAction> actions = QuotaActions.forPlan(userPlan, overageEnabled);
    if (actions.isEmpty()) {
      return;
    }

    RowLayout layout = new RowLayout(SWT.HORIZONTAL);
    layout.marginLeft = this.buttonLeftMargin; // Align with the message text
    layout.marginTop = 0;
    layout.spacing = 10;

    Composite composite = new Composite(this, SWT.NONE);
    composite.setLayout(layout);

    for (QuotaAction action : actions) {
      addButton(composite, action.label(), action.tooltip(), action.url(), action.primary());
    }
  }

  private static void addButton(Composite parent, String label, String tooltip, String link, boolean primary) {
    Button button = new Button(parent, SWT.PUSH);
    button.setText(label);
    button.setToolTipText(tooltip);
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
