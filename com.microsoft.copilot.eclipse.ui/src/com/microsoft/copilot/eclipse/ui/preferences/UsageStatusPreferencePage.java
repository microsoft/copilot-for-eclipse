package com.microsoft.copilot.eclipse.ui.preferences;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
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
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Preference page for displaying Copilot usage status including session and weekly limits.
 */
public class UsageStatusPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  public static final String ID = "com.microsoft.copilot.eclipse.ui.preferences.UsageStatusPreferencePage";

  private static final int ALIGNED_VERTICAL_SPACING = 8;
  private static final int ALIGNED_HORIZONTAL_SPACING = 16;
  private static final int DEFAULT_THRESHOLD = 90;
  private static final int[] THRESHOLD_VALUES = { 10, 20, 30, 40, 50, 60, 70, 80, 90 };
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
  private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault());
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault());
  private static final String EPOCH_TIMESTAMP = "1970-01-01T00:00:00.000Z";

  private Composite mainComposite;
  private Group accountGroup;
  private Composite accountActionsComposite;
  private Group usageGroup;
  private Label accountLabel;
  private Label planValueLabel;
  private Combo thresholdCombo;

  private enum PageLayout {
    FREE, INDIVIDUAL, CBCE_LIMITED, CBCE_UNLIMITED;

    private static PageLayout fromPlan(CopilotPlan plan, Quota premiumQuota) {
      if (plan == null) {
        return FREE;
      }
      switch (plan) {
        case free:
          return FREE;
        case individual:
        case individual_pro:
        case individual_max:
          return INDIVIDUAL;
        case business:
        case enterprise:
          return premiumQuota != null && premiumQuota.isUnlimited() ? CBCE_UNLIMITED : CBCE_LIMITED;
        default:
          return FREE;
      }
    }
  }

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

  private void scheduleQuotaLoad() {
    Job job = new Job("Loading usage status...") {
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
        String userName = StringUtils.defaultString(authManager.getUserName());
        CheckQuotaResult quotaResult = authManager.checkQuota().join();
        SwtUtils.invokeOnDisplayThreadAsync(() -> {
          if (mainComposite != null && !mainComposite.isDisposed()) {
            renderUsageStatus(userName, quotaResult);
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
    notSignedInLabel.setText(Messages.usage_status_not_signed_in);
    mainComposite.requestLayout();
  }

  private void renderUsageStatus(String userName, CheckQuotaResult quotaResult) {
    CopilotPlan plan = quotaResult != null ? quotaResult.getCopilotPlan() : null;
    String planText = plan != null ? getPlanDisplayName(plan) : "";

    renderAccountGroup(mainComposite, userName, planText);

    if (quotaResult != null) {
      PageLayout layout = PageLayout.fromPlan(plan, quotaResult.getPremiumInteractionsQuota());
      renderAccountActions(layout);
      if (hasUsageData(layout, quotaResult)) {
        renderUsageGroup(layout, quotaResult);
      }
    }

    mainComposite.requestLayout();
  }

  private String getPlanDisplayName(CopilotPlan plan) {
    switch (plan) {
      case free:
        return Messages.usage_status_plan_free;
      case individual:
      case individual_pro:
        return Messages.usage_status_plan_pro;
      case individual_max:
        return Messages.usage_status_plan_pro_plus;
      case business:
        return Messages.usage_status_plan_business;
      case enterprise:
        return Messages.usage_status_plan_enterprise;
      default:
        return Messages.usage_status_plan_unknown;
    }
  }

  private void renderAccountGroup(Composite parent, String userName, String plan) {
    accountGroup = new Group(parent, SWT.NONE);
    accountGroup.setText(Messages.usage_status_account);
    GridLayout gl = new GridLayout(2, false);
    gl.marginTop = 5;
    gl.marginLeft = 5;
    gl.verticalSpacing = ALIGNED_VERTICAL_SPACING;
    accountGroup.setLayout(gl);
    accountGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    // Account info
    accountLabel = new Label(accountGroup, SWT.NONE);
    accountLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
    accountLabel.setText(NLS.bind(Messages.usage_status_account_label, userName));

    // Plan info
    Label planLabel = new Label(accountGroup, SWT.NONE);
    planLabel.setText(Messages.usage_status_plan_prefix);
    planLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));

    planValueLabel = new Label(accountGroup, SWT.NONE);
    planValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    planValueLabel.setText(plan);
    Font boldFont = UiUtils.getBoldFont(planValueLabel.getDisplay(), planValueLabel.getFont());
    planValueLabel.setFont(boldFont);
    planValueLabel.addDisposeListener(e -> boldFont.dispose());
  }

  private void createUsageGroup(Composite parent) {
    usageGroup = new Group(parent, SWT.NONE);
    usageGroup.setText(Messages.usage_status_copilot_usage);
    GridLayout gl = new GridLayout(1, false);
    gl.marginTop = 5;
    gl.marginLeft = 5;
    gl.verticalSpacing = ALIGNED_VERTICAL_SPACING;
    usageGroup.setLayout(gl);
    usageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
  }

  private void createThresholdControls(Composite parent) {
    GridLayout thresholdLayout = new GridLayout(2, false);
    thresholdLayout.marginWidth = 0;
    thresholdLayout.marginHeight = 0;
    thresholdLayout.horizontalSpacing = 8;
    Composite thresholdComposite = new Composite(parent, SWT.NONE);
    thresholdComposite.setLayout(thresholdLayout);
    thresholdComposite.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label alertLabel = new Label(thresholdComposite, SWT.NONE);
    alertLabel.setText(Messages.usage_status_alert_threshold);

    thresholdCombo = new Combo(thresholdComposite, SWT.READ_ONLY);
    String[] thresholdLabels = new String[THRESHOLD_VALUES.length];
    for (int i = 0; i < THRESHOLD_VALUES.length; i++) {
      thresholdLabels[i] = THRESHOLD_VALUES[i] + "%";
    }
    thresholdCombo.setItems(thresholdLabels);
    int savedThreshold = getPreferenceStore().getInt(Constants.USAGE_ALERT_THRESHOLD);
    thresholdCombo.select(getThresholdIndex(savedThreshold));
  }

  private void createLimitCompound(Composite parent, String title, String tooltip, Quota quota, String resetText) {
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 4;
    layout.horizontalSpacing = ALIGNED_HORIZONTAL_SPACING;
    Composite limitCompound = new Composite(parent, SWT.NONE);
    limitCompound.setLayout(layout);
    limitCompound.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label titleLabel = new Label(limitCompound, SWT.NONE);
    titleLabel.setText(title);
    Font titleBoldFont = UiUtils.getBoldFont(titleLabel.getDisplay(), titleLabel.getFont());
    titleLabel.setFont(titleBoldFont);
    titleLabel.addDisposeListener(e -> titleBoldFont.dispose());
    titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 2, 1));

    if (StringUtils.isNotBlank(tooltip)) {
      ControlDecoration decoration = new ControlDecoration(titleLabel, SWT.RIGHT | SWT.TOP, limitCompound);
      decoration.setImage(
          FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
      decoration.setMarginWidth(3);
      decoration.setDescriptionText(tooltip);
      decoration.setShowHover(true);
      decoration.setShowOnlyOnFocus(false);
    }

    if (StringUtils.isNotBlank(resetText)) {
      Label resetLabel = new Label(limitCompound, SWT.NONE);
      resetLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));
      resetLabel.setText(resetText);
    }

    int percentUsed = quota != null ? Math.max(0, Math.min(100, (int) Math.round(100.0 - quota.getPercentRemaining())))
        : 0;

    CopilotUsageBar usageBar = new CopilotUsageBar(limitCompound, SWT.NONE);
    usageBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    usageBar.setPercentage(percentUsed);

    Label percentLabel = new Label(limitCompound, SWT.NONE);
    percentLabel.setText(formatPercentUsed(percentUsed));
    percentLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
  }

  private void createInfoCompound(Composite parent, String title, String message) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 4;
    Composite infoCompound = new Composite(parent, SWT.NONE);
    infoCompound.setLayout(layout);
    infoCompound.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

    Label titleLabel = new Label(infoCompound, SWT.NONE);
    titleLabel.setText(title);
    Font titleBoldFont = UiUtils.getBoldFont(titleLabel.getDisplay(), titleLabel.getFont());
    titleLabel.setFont(titleBoldFont);
    titleLabel.addDisposeListener(e -> titleBoldFont.dispose());

    Label messageLabel = new Label(infoCompound, SWT.WRAP);
    messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
    messageLabel.setText(message);
  }

  private void renderAccountActions(PageLayout layout) {
    if (accountActionsComposite != null && !accountActionsComposite.isDisposed()) {
      accountActionsComposite.dispose();
      accountActionsComposite = null;
    }

    if (layout != PageLayout.FREE && layout != PageLayout.INDIVIDUAL) {
      accountGroup.requestLayout();
      return;
    }

    GridLayout btnLayout = new GridLayout(2, false);
    btnLayout.marginWidth = 0;
    btnLayout.marginHeight = 0;
    btnLayout.horizontalSpacing = ALIGNED_HORIZONTAL_SPACING;
    accountActionsComposite = new Composite(accountGroup, SWT.NONE);
    accountActionsComposite.setLayout(btnLayout);
    accountActionsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false, 2, 1));

    if (layout == PageLayout.INDIVIDUAL) {
      Button configOverageBtn = new Button(accountActionsComposite, SWT.PUSH);
      configOverageBtn.setText(Messages.usage_status_configure_overage);
      configOverageBtn.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
      configOverageBtn.addListener(SWT.Selection, e -> UiUtils.openLink(Constants.GITHUB_COPILOT_SETTINGS_URL));
    }

    Button upgradePlanBtn = new Button(accountActionsComposite, SWT.PUSH);
    upgradePlanBtn.setText(Messages.usage_status_upgrade_plan);
    upgradePlanBtn.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
    upgradePlanBtn.addListener(SWT.Selection, e -> UiUtils.openLink(Constants.GITHUB_COPILOT_INDIVIDUAL_UPGRADE_URL));

    accountGroup.requestLayout();
  }

  private void renderUsageGroup(PageLayout layout, CheckQuotaResult quotaResult) {
    createUsageGroup(mainComposite);

    switch (layout) {
      case CBCE_UNLIMITED:
        createInfoCompound(usageGroup, Messages.usage_status_monthly_limit, Messages.usage_status_no_monthly_limit);
        break;
      case CBCE_LIMITED:
        createLimitCompound(usageGroup, Messages.usage_status_monthly_limit, StringUtils.EMPTY,
            quotaResult.getPremiumInteractionsQuota(), formatMonthlyReset(quotaResult.getResetDateUtc()));
        createThresholdControls(usageGroup);
        break;
      default: // FREE, INDIVIDUAL
        TbbQuota immediate = quotaResult.getImmediateUsageInterval();
        TbbQuota extended = quotaResult.getExtendedUsageInterval();

        createLimitCompound(usageGroup, Messages.usage_status_session_limit,
            Messages.usage_status_session_limit_description, immediate,
            formatSessionReset(immediate != null ? immediate.getResetAt() : null));
        createLimitCompound(usageGroup, Messages.usage_status_weekly_limit,
            Messages.usage_status_weekly_limit_description, extended,
            formatWeeklyReset(extended != null ? extended.getResetAt() : null));
        createThresholdControls(usageGroup);
        break;
    }
  }

  private boolean hasUsageData(PageLayout layout, CheckQuotaResult quotaResult) {
    if (quotaResult == null) {
      return false;
    }
    switch (layout) {
      case CBCE_UNLIMITED:
        return true;
      case CBCE_LIMITED:
        return quotaResult.getPremiumInteractionsQuota() != null;
      default:
        return quotaResult.getImmediateUsageInterval() != null && quotaResult.getExtendedUsageInterval() != null;
    }
  }

  /**
   * Formats session reset as relative time, e.g. "Resets in 1 hour 24 mins".
   */
  private String formatSessionReset(String utcTimestamp) {
    if (StringUtils.isBlank(utcTimestamp) || EPOCH_TIMESTAMP.equals(utcTimestamp)) {
      return "";
    }
    try {
      Duration remaining = Duration.between(Instant.now(), Instant.parse(utcTimestamp));
      if (remaining.isNegative() || remaining.isZero()) {
        return NLS.bind(Messages.usage_status_resets_in, NLS.bind(Messages.usage_status_duration_mins, 0));
      }
      long hours = remaining.toHours();
      long mins = remaining.toMinutesPart();
      String duration;
      if (hours > 1 && mins > 0) {
        duration = NLS.bind(Messages.usage_status_duration_hours_mins, hours, mins);
      } else if (hours == 1 && mins > 0) {
        duration = NLS.bind(Messages.usage_status_duration_hour_mins, mins);
      } else if (hours > 1) {
        duration = NLS.bind(Messages.usage_status_duration_hours, hours);
      } else if (hours == 1) {
        duration = Messages.usage_status_duration_hour;
      } else {
        duration = NLS.bind(Messages.usage_status_duration_mins, mins);
      }
      return NLS.bind(Messages.usage_status_resets_in, duration);
    } catch (DateTimeParseException e) {
      CopilotCore.LOGGER.error("Failed to parse session reset date: " + utcTimestamp, e);
      return "";
    }
  }

  /**
   * Formats weekly reset as day-of-week + time, e.g. "Resets on Monday at 12:34 PM".
   */
  private String formatWeeklyReset(String utcTimestamp) {
    if (StringUtils.isBlank(utcTimestamp) || EPOCH_TIMESTAMP.equals(utcTimestamp)) {
      return "";
    }
    try {
      ZonedDateTime localTime = Instant.parse(utcTimestamp).atZone(ZoneId.systemDefault());
      return NLS.bind(Messages.usage_status_resets_on_at, localTime.format(DAY_FORMATTER),
          localTime.format(TIME_FORMATTER));
    } catch (DateTimeParseException e) {
      CopilotCore.LOGGER.error("Failed to parse weekly reset date: " + utcTimestamp, e);
      return "";
    }
  }

  /**
   * Formats monthly reset as month + day + time, e.g. "Resets on July 7 at 8:00 AM".
   */
  private String formatMonthlyReset(String utcTimestamp) {
    if (StringUtils.isBlank(utcTimestamp) || EPOCH_TIMESTAMP.equals(utcTimestamp)) {
      return "";
    }
    try {
      ZonedDateTime localTime = Instant.parse(utcTimestamp).atZone(ZoneId.systemDefault());
      return NLS.bind(Messages.usage_status_resets_on_at, localTime.format(DATE_FORMATTER),
          localTime.format(TIME_FORMATTER));
    } catch (DateTimeParseException e) {
      CopilotCore.LOGGER.error("Failed to parse monthly reset date: " + utcTimestamp, e);
      return "";
    }
  }

  private static String formatPercentUsed(int percent) {
    return percent + "%";
  }

  private int getThresholdIndex(int threshold) {
    for (int i = 0; i < THRESHOLD_VALUES.length; i++) {
      if (THRESHOLD_VALUES[i] == threshold) {
        return i;
      }
    }
    // Return index for default (90%)
    return THRESHOLD_VALUES.length - 1;
  }

  @Override
  public boolean performOk() {
    if (thresholdCombo != null && !thresholdCombo.isDisposed()) {
      int selectedIndex = thresholdCombo.getSelectionIndex();
      if (selectedIndex >= 0 && selectedIndex < THRESHOLD_VALUES.length) {
        getPreferenceStore().setValue(Constants.USAGE_ALERT_THRESHOLD, THRESHOLD_VALUES[selectedIndex]);
      }
    }
    return super.performOk();
  }

  @Override
  protected void performDefaults() {
    if (thresholdCombo != null && !thresholdCombo.isDisposed()) {
      thresholdCombo.select(getThresholdIndex(DEFAULT_THRESHOLD));
    }
    super.performDefaults();
  }
}
