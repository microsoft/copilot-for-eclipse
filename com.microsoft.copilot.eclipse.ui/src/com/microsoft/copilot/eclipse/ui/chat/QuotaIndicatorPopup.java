package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.QuotaChangeNotification;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.QuotaSnapshotParams;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;
import com.microsoft.copilot.eclipse.ui.preferences.UsageStatusPreferencePage;
import com.microsoft.copilot.eclipse.ui.utils.PreferencesUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Popup that shows a quick summary of Copilot quota usage when hovering the {@link QuotaUsageIndicator}.
 */
class QuotaIndicatorPopup extends BaseHoverPopup {

  private static final String POPUP_ACTION_TEXT_CLASS = "popup-action-text";

  private QuotaChangeNotification lastNotification;

  QuotaIndicatorPopup() {
    super();
  }

  void open(Control anchor, QuotaChangeNotification notification) {
    this.anchor = anchor;
    this.lastNotification = notification;
    if (notification == null) {
      return;
    }
    openPopup(anchor);
  }

  protected void updateNotification(QuotaChangeNotification notification) {
    this.lastNotification = notification;
  }

  @Override
  protected void populateContent(Composite parent) {
    addSectionHeader(parent, Messages.quota_popup_title, 0);

    CopilotPlan plan = lastNotification.copilotPlan();
    boolean isCbce = plan == CopilotPlan.business || plan == CopilotPlan.enterprise;

    if (isCbce) {
      buildCbceContent(parent, lastNotification);
    } else {
      buildCfiContent(parent, lastNotification);
    }

    addSeparator(parent, SECTION_SPACING);
    addClickToViewDetails(parent);
  }

  private void buildCbceContent(Composite parent, QuotaChangeNotification notification) {
    QuotaSnapshotParams premium = notification.premiumInteractions();
    if (premium != null && premium.unlimited()) {
      addKeyValueRow(parent, Messages.quota_popup_monthly_limit, Messages.quota_popup_unlimited);
    } else {
      double percentUsed = premium != null ? toOneDecimal(100.0 - premium.percentRemaining()) : 0.0;
      addKeyValueRow(parent, Messages.quota_popup_monthly_limit,
          NLS.bind(Messages.quota_popup_percent_used_suffix, percentUsed));
    }
  }

  private void buildCfiContent(Composite parent, QuotaChangeNotification notification) {
    QuotaSnapshotParams immediate = notification.immediateUsageInterval();
    QuotaSnapshotParams extended = notification.extendedUsageInterval();

    if (immediate != null) {
      if (immediate.unlimited()) {
        addKeyValueRow(parent, Messages.quota_popup_session_limit, Messages.quota_popup_unlimited);
      } else {
        double sessionUsed = toOneDecimal(100.0 - immediate.percentRemaining());
        addKeyValueRow(parent, Messages.quota_popup_session_limit,
            NLS.bind(Messages.quota_popup_percent_used_suffix, sessionUsed));
      }
    }
    if (extended != null) {
      if (extended.unlimited()) {
        addKeyValueRow(parent, Messages.quota_popup_weekly_limit, Messages.quota_popup_unlimited);
      } else {
        double weeklyUsed = toOneDecimal(100.0 - extended.percentRemaining());
        addKeyValueRow(parent, Messages.quota_popup_weekly_limit,
            NLS.bind(Messages.quota_popup_percent_used_suffix, weeklyUsed));
      }
    }
  }

  private static double toOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private void addClickToViewDetails(Composite parent) {
    Label link = new Label(parent, SWT.NONE);
    link.setText(Messages.quota_popup_click_to_view_details);
    applyCssId(link, DROPDOWN_POPUP_CSS_ID);
    UiUtils.applyCssClass(link, POPUP_ACTION_TEXT_CLASS, stylingEngine);
    link.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    link.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    link.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        Shell preferencesShell = anchor != null && !anchor.isDisposed() ? anchor.getShell()
            : PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null
                ? PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                : parent.getShell();
        close();
        PreferencesUtil.createPreferenceDialogOn(preferencesShell, UsageStatusPreferencePage.ID,
            PreferencesUtils.getAllPreferenceIds(), null).open();
      }
    });
  }
}
