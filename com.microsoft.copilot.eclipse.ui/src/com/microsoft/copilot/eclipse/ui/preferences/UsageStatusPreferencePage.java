package com.microsoft.copilot.eclipse.ui.preferences;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.TbbQuota;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.swt.CopilotUsageBar;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Preference page for displaying Copilot usage including session and weekly limits.
 */
public class UsageStatusPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.UsageStatusPreferencePage";

  private static final int ALIGNED_VERTICAL_SPACING = 8;
  private static final int BUTTON_HORIZONTAL_PADDING = 10;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault());

  private Composite mainComposite;
  private final List<CopilotUsageBar> usageBars = new ArrayList<>();

  @Override
  public void init(IWorkbench workbench) {
    setPreferenceStore(CopilotUi.getPlugin().getPreferenceStore());
  }

  @Override
  protected Control createContents(Composite parent) {
    mainComposite = new Composite(parent, SWT.NONE);
    GridLayout mainLayout = new GridLayout(1, false);
    mainLayout.marginWidth = 0;
    mainLayout.marginHeight = 0;
    mainComposite.setLayout(mainLayout);
    mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    scheduleQuotaLoad();

    return mainComposite;
  }

  @Override
  public boolean performOk() {
    for (CopilotUsageBar bar : usageBars) {
      if (!bar.isDisposed()) {
        bar.refreshBar();
      }
    }
    return super.performOk();
  }

  private void scheduleQuotaLoad() {
    Job job = new Job("Loading usage...") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          Job.getJobManager().join(CopilotUi.INIT_JOB_FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
          CopilotCore.LOGGER.error(e);
        }
        AuthStatusManager authManager = CopilotCore.getPlugin().getAuthStatusManager();
        if (authManager == null || authManager.isNotSignedInOrNotAuthorized()) {
          SwtUtils.invokeOnDisplayThreadAsync(() -> renderNotSignedIn());
          return Status.OK_STATUS;
        }
        CheckQuotaResult quotaResult = authManager.checkQuota().join();
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          if (mainComposite != null && !mainComposite.isDisposed()) {
            renderUsageStatus(quotaResult);
          }
        });
        return Status.OK_STATUS;
      }
    };
    job.setUser(true);
    job.schedule();
  }

  private void renderNotSignedIn() {
    if (mainComposite == null || mainComposite.isDisposed()) {
      return;
    }
    Label notSignedInLabel = new Label(mainComposite, SWT.WRAP);
    notSignedInLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    notSignedInLabel.setText(Messages.usage_not_signed_in);
    mainComposite.requestLayout();
  }

  private void renderUsageStatus(CheckQuotaResult quotaResult) {
    if (quotaResult != null) {
      CopilotPlan plan = quotaResult.getCopilotPlan();
      Group group = createUsageGroup(mainComposite);
      createPlanLabel(group, plan != null ? getPlanDisplayName(plan) : StringUtils.EMPTY);

      PageLayout layout = PageLayout.fromPlan(plan, quotaResult.getPremiumInteractionsQuota());
      renderUsageLayout(group, layout, quotaResult);
      createActionButtons(group, layout);
    }

    mainComposite.requestLayout();
  }

  private Group createUsageGroup(Composite parent) {
    Group group = new Group(parent, SWT.NONE);
    group.setText(Messages.usage_copilot_usage);
    GridLayout gl = new GridLayout(1, false);
    gl.marginTop = 0;
    gl.marginLeft = 5;
    gl.verticalSpacing = ALIGNED_VERTICAL_SPACING;
    group.setLayout(gl);
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    return group;
  }

  private void createPlanLabel(Composite parent, String planText) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.marginBottom = ALIGNED_VERTICAL_SPACING;
    Composite planComposite = new Composite(parent, SWT.NONE);
    planComposite.setLayout(layout);
    planComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label planLabel = new Label(planComposite, SWT.NONE);
    planLabel.setText(planText);
    GridData planData = new GridData(SWT.LEFT, SWT.NONE, true, false);
    planData.verticalIndent = ALIGNED_VERTICAL_SPACING;
    planLabel.setLayoutData(planData);
    applyBoldFont(planLabel);
  }

  private void renderUsageLayout(Group group, PageLayout layout, CheckQuotaResult quotaResult) {
    switch (layout) {
      case CBCE_UNLIMITED:
        createInfoCompound(group, Messages.usage_monthly_limit, Messages.usage_no_monthly_limit);
        break;
      case CBCE_LIMITED:
        createBusinessEnterpriseUsage(group, quotaResult);
        break;
      default: // FREE, INDIVIDUAL, INDIVIDUAL_PRO, INDIVIDUAL_MAX
        createIndividualUsage(group, quotaResult);
        break;
    }
  }

  private void createInfoCompound(Composite parent, String title, String message) {
    Label titleLabel = new Label(parent, SWT.NONE);
    titleLabel.setText(title);
    titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    applyBoldFont(titleLabel);

    Label messageLabel = new Label(parent, SWT.WRAP);
    messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    messageLabel.setText(message);
  }

  private void createBusinessEnterpriseUsage(Composite parent, CheckQuotaResult quotaResult) {
    createLimitCompound(parent, Messages.usage_monthly_limit, Messages.usage_monthly_limit_description,
        quotaResult.getPremiumInteractionsQuota(), formatDateReset(quotaResult.getResetDateUtc(), DATE_FORMATTER));
  }

  private void createIndividualUsage(Composite parent, CheckQuotaResult quotaResult) {
    TbbQuota sessionQuota = quotaResult.getImmediateUsageInterval();
    TbbQuota weeklyQuota = quotaResult.getExtendedUsageInterval();

    if (sessionQuota != null) {
      createLimitCompound(parent, Messages.usage_session_limit, Messages.usage_session_limit_description, sessionQuota,
          formatSessionReset(sessionQuota.getResetAt()));
    }
    if (weeklyQuota != null) {
      createLimitCompound(parent, Messages.usage_weekly_limit, Messages.usage_weekly_limit_description, weeklyQuota,
          formatDateReset(weeklyQuota.getResetAt(), DAY_FORMATTER));
    }
    createOverageLabel(parent, sessionQuota, weeklyQuota);
  }

  private void createLimitCompound(Composite parent, String title, String tooltip, Quota quota, String hintText) {
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.marginBottom = ALIGNED_VERTICAL_SPACING;
    layout.verticalSpacing = 0;
    Composite limitCompound = new Composite(parent, SWT.NONE);
    limitCompound.setLayout(layout);
    limitCompound.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label titleLabel = new Label(limitCompound, SWT.NONE);
    titleLabel.setText(title);
    applyBoldFont(titleLabel);
    titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 2, 1));

    if (StringUtils.isNotBlank(tooltip)) {
      UiUtils.addTooltipDecoration(titleLabel, limitCompound, tooltip);
    }

    if (quota != null && quota.isUnlimited()) {
      Label unlimitedLabel = new Label(limitCompound, SWT.WRAP);
      unlimitedLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
      unlimitedLabel.setText(getUnlimitedMessage(title));
      return;
    }

    int percentUsed = quota != null ? Math.max(0, Math.min(100, (int) Math.round(100.0 - quota.getPercentRemaining())))
        : 0;

    CopilotUsageBar usageBar = new CopilotUsageBar(limitCompound, SWT.NONE);
    usageBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    usageBar.setPercentage(percentUsed);
    usageBars.add(usageBar);

    Label percentLabel = new Label(limitCompound, SWT.RIGHT);
    percentLabel.setText(NLS.bind(Messages.usage_percentage_suffix, percentUsed));
    GridData percentData = new GridData(SWT.END, SWT.CENTER, false, false);
    GC gc = new GC(percentLabel);
    percentData.widthHint = gc.textExtent(NLS.bind(Messages.usage_percentage_suffix, 100)).x;
    gc.dispose();
    percentLabel.setLayoutData(percentData);

    if (StringUtils.isNotBlank(hintText)) {
      Label resetLabel = new Label(limitCompound, SWT.NONE);
      resetLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
      resetLabel.setText(hintText);
    }
  }

  private void createOverageLabel(Composite parent, TbbQuota sessionQuota, TbbQuota weeklyQuota) {
    Label overageLabel = new Label(parent, SWT.NONE);
    overageLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));
    boolean overagePermitted = (sessionQuota != null && sessionQuota.isOveragePermitted())
        || (weeklyQuota != null && weeklyQuota.isOveragePermitted());
    int totalOverageCount = 0;
    if (sessionQuota != null) {
      totalOverageCount += sessionQuota.getOverageCount();
    }
    if (weeklyQuota != null) {
      totalOverageCount += weeklyQuota.getOverageCount();
    }

    if (overagePermitted) {
      overageLabel.setText(Messages.usage_overage_configured);
      overageLabel.setForeground(CssConstants.getInputPlaceHolderColor(parent.getDisplay()));
    } else if (totalOverageCount == 0) {
      overageLabel.setText(Messages.usage_overage_not_configured);
      overageLabel.setForeground(CssConstants.getInputPlaceHolderColor(parent.getDisplay()));
    }

    UiUtils.addTooltipDecoration(overageLabel, parent, Messages.usage_overage_spend_tooltip);
  }

  /**
   * Renders action buttons at the bottom of the usage group.
   *
   * <p>Button visibility by layout:
   * <ul>
   * <li>{@code FREE} — Upgrade Plan only</li>
   * <li>{@code INDIVIDUAL_PRO} — Manage your budget + Upgrade Plan</li>
   * <li>{@code INDIVIDUAL_MAX} — Manage your budget only (already on max plan)</li>
   * <li>{@code CBCE_LIMITED / CBCE_UNLIMITED} — no buttons</li>
   * </ul>
   *
   * <p>The first button rendered is always styled as the primary CTA.
   */
  private void createActionButtons(Composite parent, PageLayout layout) {
    if (layout != PageLayout.FREE && layout != PageLayout.INDIVIDUAL_PRO && layout != PageLayout.INDIVIDUAL_MAX) {
      return;
    }

    GridLayout btnLayout = new GridLayout(2, false);
    btnLayout.marginWidth = 0;
    btnLayout.marginHeight = 0;
    Composite actionsComposite = new Composite(parent, SWT.NONE);
    actionsComposite.setLayout(btnLayout);
    actionsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));

    boolean hasManageBudget = layout == PageLayout.INDIVIDUAL_PRO || layout == PageLayout.INDIVIDUAL_MAX;

    if (hasManageBudget) {
      Button manageBudgetBtn = new Button(actionsComposite, SWT.PUSH);
      manageBudgetBtn.setText(Messages.usage_manage_budget);
      manageBudgetBtn.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
      GridData manageBtnData = new GridData(SWT.LEFT, SWT.NONE, false, false);
      manageBtnData.widthHint = manageBudgetBtn.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + BUTTON_HORIZONTAL_PADDING;
      manageBudgetBtn.setLayoutData(manageBtnData);
      manageBudgetBtn.addListener(SWT.Selection, e -> UiUtils.openLink(Constants.GITHUB_COPILOT_SETTINGS_URL));
    }

    if (layout != PageLayout.INDIVIDUAL_MAX) {
      Button upgradePlanBtn = new Button(actionsComposite, SWT.PUSH);
      upgradePlanBtn.setText(Messages.usage_upgrade_plan);
      if (!hasManageBudget) {
        upgradePlanBtn.setData(CssConstants.CSS_CLASS_NAME_KEY, "btn-primary");
      }
      GridData upgradeBtnData = new GridData(SWT.LEFT, SWT.NONE, false, false);
      upgradeBtnData.widthHint = upgradePlanBtn.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + BUTTON_HORIZONTAL_PADDING;
      upgradePlanBtn.setLayoutData(upgradeBtnData);
      upgradePlanBtn.addListener(SWT.Selection, e -> UiUtils.openLink(Constants.GITHUB_COPILOT_INDIVIDUAL_UPGRADE_URL));
    }
  }

  private static String getPlanDisplayName(CopilotPlan plan) {
    switch (plan) {
      case free:
        return Messages.usage_plan_free;
      case individual:
        return Messages.usage_plan_pro;
      case individual_pro:
        return Messages.usage_plan_pro_plus;
      case individual_max:
        return Messages.usage_plan_max;
      case business:
        return Messages.usage_plan_business;
      case enterprise:
        return Messages.usage_plan_enterprise;
      default:
        return Messages.usage_plan_unknown;
    }
  }

  /**
   * Formats session reset as relative time, e.g. "Resets in 1 hour 24 mins".
   */
  private static String formatSessionReset(String utcTimestamp) {
    return formatTimestamp(utcTimestamp, resetAt -> {
      Duration remaining = Duration.between(Instant.now(), resetAt);
      if (remaining.isNegative() || remaining.isZero()) {
        return NLS.bind(Messages.usage_resets_in, NLS.bind(Messages.usage_duration_mins, 0));
      }
      long hours = remaining.toHours();
      long mins = remaining.toMinutesPart();
      String duration;
      if (hours > 1 && mins > 0) {
        duration = NLS.bind(Messages.usage_duration_hours_mins, hours, mins);
      } else if (hours == 1 && mins > 0) {
        duration = NLS.bind(Messages.usage_duration_hour_mins, mins);
      } else if (hours > 1) {
        duration = NLS.bind(Messages.usage_duration_hours, hours);
      } else if (hours == 1) {
        duration = Messages.usage_duration_hour;
      } else {
        duration = NLS.bind(Messages.usage_duration_mins, mins);
      }
      return NLS.bind(Messages.usage_resets_in, duration);
    });
  }

  /**
   * Formats a date reset as formatted date + time, e.g. "Resets on Monday at 12:34 PM" or "Resets on July 7 at 8:00
   * AM".
   */
  private static String formatDateReset(String utcTimestamp, DateTimeFormatter dateFormatter) {
    return formatTimestamp(utcTimestamp, resetAt -> {
      ZonedDateTime localTime = resetAt.atZone(ZoneId.systemDefault());
      return NLS.bind(Messages.usage_resets_on_at, localTime.format(dateFormatter), localTime.format(TIME_FORMATTER));
    });
  }

  private static String formatTimestamp(String utcTimestamp, TimestampFormatter formatter) {
    if (StringUtils.isBlank(utcTimestamp)) {
      return "";
    }
    try {
      Instant timestamp = Instant.parse(utcTimestamp);
      if (Instant.EPOCH.equals(timestamp)) {
        return Messages.usage_resets_no_usage_yet;
      }
      return formatter.format(timestamp);
    } catch (DateTimeParseException e) {
      CopilotCore.LOGGER.error("Failed to parse reset date: " + utcTimestamp, e);
      return "";
    }
  }

  @FunctionalInterface
  private interface TimestampFormatter {
    String format(Instant utcTimestamp);
  }

  private static String getUnlimitedMessage(String title) {
    if (Messages.usage_session_limit.equals(title)) {
      return Messages.usage_no_session_limit;
    } else if (Messages.usage_weekly_limit.equals(title)) {
      return Messages.usage_no_weekly_limit;
    }
    return Messages.usage_no_monthly_limit;
  }

  private static void applyBoldFont(Label label) {
    Font boldFont = UiUtils.getBoldFont(label.getDisplay(), label.getFont());
    label.setFont(boldFont);
    label.addDisposeListener(e -> boldFont.dispose());
  }

  private enum PageLayout {
    FREE, INDIVIDUAL_PRO, INDIVIDUAL_MAX, CBCE_LIMITED, CBCE_UNLIMITED;

    private static PageLayout fromPlan(CopilotPlan plan, Quota premiumQuota) {
      if (plan == null) {
        return FREE;
      }
      switch (plan) {
        case free:
          return FREE;
        case individual:
        case individual_pro:
          return INDIVIDUAL_PRO;
        case individual_max:
          return INDIVIDUAL_MAX;
        case business:
        case enterprise:
          return premiumQuota != null && premiumQuota.isUnlimited() ? CBCE_UNLIMITED : CBCE_LIMITED;
        default:
          return FREE;
      }
    }
  }
}
