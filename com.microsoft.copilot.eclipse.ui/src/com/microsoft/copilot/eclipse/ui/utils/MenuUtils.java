// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.Quota;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Shared helpers used by the Copilot status-bar and menu-bar usage menus.
 */
public final class MenuUtils {

  private static final DateTimeFormatter ALLOWANCE_RESET_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("MMMM d, yyyy");

  private MenuUtils() {
  }

  /**
   * Returns the localized plan label for the given plan, or {@code null} if the plan is unknown.
   */
  public static String getPlanLabel(CopilotPlan plan) {
    if (plan == null) {
      return null;
    }
    switch (plan) {
      case free:
        return Messages.menu_quota_plan_free;
      case individual:
        return Messages.menu_quota_plan_individual;
      case individual_pro:
        return Messages.menu_quota_plan_individualPro;
      case individual_max:
        return Messages.menu_quota_plan_individualMax;
      case business:
        return Messages.menu_quota_plan_business;
      case enterprise:
        return Messages.menu_quota_plan_enterprise;
      default:
        return null;
    }
  }

  /**
   * Returns the percent-remaining used to pick the usage icon, based on the user's plan.
   */
  public static double calculatePercentRemaining(CheckQuotaResult quotaStatus) {
    CopilotPlan plan = quotaStatus.copilotPlan();
    Quota premiumQuota = quotaStatus.premiumInteractions();
    if (plan == CopilotPlan.free) {
      Quota completionsQuota = quotaStatus.completions();
      Quota chatQuota = quotaStatus.chat();
      if (completionsQuota == null || chatQuota == null) {
        return 100;
      }
      return Math.min(completionsQuota.percentRemaining(), chatQuota.percentRemaining());
    }
    if (premiumQuota != null) {
      return premiumQuota.percentRemaining();
    }
    return 100;
  }

  /**
   * Returns the image descriptor for the usage row based on the lowest percentRemaining.
   */
  public static ImageDescriptor getUsageIcon(double percentRemaining) {
    if (percentRemaining <= 10) {
      return UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_red.png");
    }
    if (percentRemaining <= 25) {
      return UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_yellow.png");
    }
    return UiUtils.buildImageDescriptorFromPngPath("/icons/quota/usage_blue.png");
  }

  /**
   * Returns the shared blank icon descriptor used for indented usage rows.
   */
  public static ImageDescriptor getBlankIcon() {
    return UiUtils.buildImageDescriptorFromPngPath("/icons/blank.png");
  }

  /**
   * True when the user is on a Business / Enterprise plan with no monthly premium-interactions limit.
   */
  public static boolean isOrgUnlimited(CheckQuotaResult quotaStatus) {
    CopilotPlan plan = quotaStatus.copilotPlan();
    Quota premiumQuota = quotaStatus.premiumInteractions();
    return (plan == CopilotPlan.business || plan == CopilotPlan.enterprise)
        && premiumQuota != null && premiumQuota.unlimited();
  }

  /**
   * True when the user has a non-org premium-interactions quota - i.e. a paid plan with a
   * populated, non-org-unlimited {@link CheckQuotaResult#premiumInteractions()}. This single
   * predicate gates both the Monthly limit display row and the overage upsell row ("Enable
   * Additional Usage" / "Increase Budget"): without metered premium data the upsell has no data
   * to act on and would mislead the user.
   */
  public static boolean hasNonOrgPremiumQuota(CheckQuotaResult quotaStatus) {
    if (quotaStatus.copilotPlan() == CopilotPlan.free) {
      return false;
    }
    if (isOrgUnlimited(quotaStatus)) {
      return false;
    }
    return quotaStatus.premiumInteractions() != null;
  }

  /**
   * True when the "Upgrade Plan" row should be shown for the given plan.
   */
  public static boolean shouldShowUpgradePlanRow(CopilotPlan plan) {
    return plan == CopilotPlan.free || plan == CopilotPlan.individual || plan == CopilotPlan.individual_pro;
  }

  /**
   * True when the plan is a CFI (Copilot for Individuals) plan: individual, individual_pro, or
   * individual_max.
   */
  public static boolean isCfiPlan(CopilotPlan plan) {
    return plan == CopilotPlan.individual || plan == CopilotPlan.individual_pro
        || plan == CopilotPlan.individual_max;
  }

  /**
   * Returns the label for the overage upsell row depending on the current overage state.
   */
  public static String getOverageRowLabel(Quota premiumQuota) {
    boolean overageEnabled = premiumQuota != null && premiumQuota.overagePermitted();
    return overageEnabled ? Messages.menu_quota_increaseBudget : Messages.menu_quota_enableAdditionalUsage;
  }

  /**
   * True when the allowance-reset row should be shown. The row is hidden when there is no monthly
   * allowance to reset (premium-interactions quota is unlimited), when no reset date was supplied,
   * or when the supplied reset date cannot be parsed.
   */
  public static boolean shouldShowAllowanceResetRow(CheckQuotaResult quotaStatus) {
    Quota premiumQuota = quotaStatus.premiumInteractions();
    if (premiumQuota != null && premiumQuota.unlimited()) {
      return false;
    }
    return parseResetDate(quotaStatus).isPresent();
  }

  /**
   * True when none of the quotas tracked for the user's plan have any usage yet. For free plans this
   * means both the chat and completions quotas are at 0% used; for paid plans this means the premium
   * interactions quota is at 0% used.
   */
  public static boolean noUsageYet(CheckQuotaResult quotaStatus) {
    if (quotaStatus.copilotPlan() == CopilotPlan.free) {
      return isUnused(quotaStatus.chat()) && isUnused(quotaStatus.completions());
    }
    return isUnused(quotaStatus.premiumInteractions());
  }

  /**
   * Formats the allowance-reset row label. Returns {@link Messages#menu_quota_noUsageYet} when the
   * user has not consumed any of the tracked quotas yet (see {@link #noUsageYet}); otherwise
   * returns {@code "Resets today"} / {@code "Reset in 1 day on {date}"} / {@code "Reset in {n}
   * days on {date}"} depending on {@code n}. The 0-day case intentionally omits the date since it
   * is implied by "today".
   *
   * <p>Callers <strong>must</strong> gate with {@link #shouldShowAllowanceResetRow}; this method
   * assumes a parseable reset date is present.
   */
  public static String formatAllowanceReset(CheckQuotaResult quotaStatus) {
    if (noUsageYet(quotaStatus)) {
      return Messages.menu_quota_noUsageYet;
    }
    LocalDate resetDate = parseResetDate(quotaStatus).orElseThrow(
        () -> new IllegalStateException("formatAllowanceReset called without a parseable reset date; "
            + "callers must gate with shouldShowAllowanceResetRow"));
    long days = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), resetDate));
    String formattedDate = resetDate.format(ALLOWANCE_RESET_DATE_FORMATTER);
    if (days == 0) {
      return Messages.menu_quota_allowanceReset_today;
    }
    if (days == 1) {
      return NLS.bind(Messages.menu_quota_allowanceReset_singular, formattedDate);
    }
    return NLS.bind(Messages.menu_quota_allowanceReset_plural, days, formattedDate);
  }

  /**
   * Parses the reset date supplied by the language server, preferring {@code resetDateUtc} (an ISO
   * instant resolved against the local time zone) over {@code resetDate} (a local ISO date) since
   * the instant form carries more precision. Returns an empty optional when neither field is
   * parseable.
   */
  private static Optional<LocalDate> parseResetDate(CheckQuotaResult quotaStatus) {
    String utc = quotaStatus.resetDateUtc();
    if (StringUtils.isNotBlank(utc)) {
      try {
        return Optional.of(Instant.parse(utc).atZone(ZoneId.systemDefault()).toLocalDate());
      } catch (DateTimeParseException e) {
        CopilotCore.LOGGER.error("Unparseable quota resetDateUtc: " + utc, e);
      }
    }
    String local = quotaStatus.resetDate();
    if (StringUtils.isNotBlank(local)) {
      try {
        return Optional.of(LocalDate.parse(local));
      } catch (DateTimeParseException e) {
        CopilotCore.LOGGER.error("Unparseable quota resetDate: " + local, e);
      }
    }
    return Optional.empty();
  }

  /**
   * True when the quota has been measured but no allowance has been consumed yet (matches the
   * {@code "0% used"} display threshold in {@code QuotaTextCalculator#getPercentUsed}). Unlimited
   * quotas are treated as "used" so that the no-usage message is not shown when the limit is
   * irrelevant.
   */
  private static boolean isUnused(Quota quota) {
    if (quota == null || quota.unlimited()) {
      return false;
    }
    return (100 - quota.percentRemaining()) < 0.1;
  }
}
