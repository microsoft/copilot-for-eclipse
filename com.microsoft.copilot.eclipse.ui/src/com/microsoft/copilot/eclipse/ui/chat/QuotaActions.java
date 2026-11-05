// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CopilotPlan;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Plan-driven call-to-action list shown when the user's quota is approaching or exceeded. Shared by the
 * {@link StaticBanner}-based quota notification (rendered via {@code ActionBar#createQuotaWarningBanner})
 * and the inline {@link WarnWidget} rendered under a chat turn on a 402 error response, so both surfaces
 * stay in sync when a new plan or call-to-action is added.
 */
public final class QuotaActions {

  /**
   * Single quota call-to-action.
   *
   * @param label visible button or link label
   * @param tooltip tooltip text used by the inline {@link WarnWidget}; ignored by {@link StaticBanner}
   * @param url target URL opened on activation
   * @param primary {@code true} when this action should be visually emphasised (e.g. {@code btn-primary}
   *     styling on a push button); ignored by {@link StaticBanner}, which renders all actions as links
   */
  public record QuotaAction(String label, String tooltip, String url, boolean primary) {
  }

  private QuotaActions() {
  }

  /**
   * The plan inputs needed to build quota actions, extracted from a {@link CheckQuotaResult}. Shared
   * by the browser renderer and the StyledText {@link WarnWidget} path so the {@code CheckQuotaResult}
   * decomposition lives in one place.
   *
   * @param plan the user's Copilot plan, or {@code null} when unknown
   * @param overageEnabled {@code true} when additional paid usage is already enabled for the user
   * @param canUpgradePlan whether the user can upgrade their plan, or {@code null} when the language
   *     server did not supply this field
   */
  public record QuotaPlanContext(CopilotPlan plan, boolean overageEnabled, Boolean canUpgradePlan) {

    /** Extracts the plan inputs from a language-server {@link CheckQuotaResult}. */
    public static QuotaPlanContext from(CheckQuotaResult quotaStatus) {
      boolean overageEnabled = quotaStatus.premiumInteractions() != null
          && quotaStatus.premiumInteractions().overagePermitted();
      return new QuotaPlanContext(quotaStatus.copilotPlan(), overageEnabled,
          quotaStatus.canUpgradePlan());
    }
  }

  /**
   * Resolves the {@link QuotaAction}s for a quota-exceeded error given the current quota status.
   * Returns an empty list for non-{@code 402} errors, BYOK quota errors, when token-based billing is
   * not enabled, or when {@code quotaStatus} is {@code null}. Otherwise delegates to
   * {@link #forPlan(CopilotPlan, boolean, Boolean)} with the inputs from
   * {@link QuotaPlanContext#from(CheckQuotaResult)}.
   *
   * @param quotaStatus the current quota status, or {@code null}
   * @param code the language-server error code
   * @param modelProviderName the BYOK model-provider name, or {@code null}
   * @return an immutable, possibly empty list; never {@code null}
   */
  public static List<QuotaAction> forQuotaStatus(CheckQuotaResult quotaStatus, int code,
      String modelProviderName) {
    if (code != 402 || isByokQuotaExceeded(code, modelProviderName)) {
      return List.of();
    }
    if (quotaStatus == null || !quotaStatus.tokenBasedBillingEnabled()) {
      return List.of();
    }
    QuotaPlanContext context = QuotaPlanContext.from(quotaStatus);
    return forPlan(context.plan(), context.overageEnabled(), context.canUpgradePlan());
  }

  /**
   * Returns the ordered list of {@link QuotaAction}s appropriate for the supplied plan.
   *
   * <p>Non-upgrade actions are derived from the plan:
   * <ul>
   *   <li>{@code free} &rarr; none</li>
   *   <li>{@code individual}, {@code individual_pro}, {@code individual_max} &rarr;
   *       "Enable Additional Usage" / "Increase Budget"</li>
   *   <li>{@code business}, {@code enterprise} &rarr; none</li>
   * </ul>
   *
   * <p>The "Upgrade Plan" action is then appended when the user is eligible. When
   * {@code canUpgradePlan} is non-{@code null} it takes precedence over the plan-based default;
   * otherwise the plan default is used (eligible for {@code free}, {@code individual},
   * {@code individual_pro}). When the upgrade action is the only entry it is rendered as primary;
   * when it follows another primary action it is rendered as secondary so the surfaces stay
   * visually consistent.
   *
   * <p>The "Enable Additional Usage" label is replaced with "Increase Budget" when
   * {@code overageEnabled} is {@code true}, matching the IntelliJ quota dialog wording.
   *
   * @param plan the user's Copilot plan, or {@code null} when unknown
   * @param overageEnabled {@code true} when additional paid usage is already enabled for the user
   * @param canUpgradePlan whether the user can upgrade their Copilot plan, or {@code null} when the
   *     language server did not supply this field
   * @return an immutable, possibly empty list; never {@code null}
   */
  public static List<QuotaAction> forPlan(CopilotPlan plan, boolean overageEnabled, Boolean canUpgradePlan) {
    if (plan == null) {
      return List.of();
    }
    QuotaAction upgradePrimary = new QuotaAction(Messages.menu_quota_upgradePlan,
        Messages.chat_noQuotaView_updatePlanButton_Tooltip,
        UiConstants.COPILOT_UPGRADE_PLAN_URL, true);
    QuotaAction upgradeSecondary = new QuotaAction(Messages.menu_quota_upgradePlan,
        Messages.chat_noQuotaView_updatePlanButton_Tooltip,
        UiConstants.COPILOT_UPGRADE_PLAN_URL, false);
    String overageLabel = overageEnabled ? Messages.menu_quota_increaseBudget
        : Messages.menu_quota_enableAdditionalUsage;
    QuotaAction manageOverage = new QuotaAction(overageLabel,
        Messages.chat_noQuotaView_enableAdditionalUsageButton_tooltip,
        UiConstants.MANAGE_COPILOT_OVERAGE_URL, true);

    boolean hasOverage = switch (plan) {
      case individual, individual_pro, individual_max -> true;
      case free, business, enterprise -> false;
    };
    boolean showUpgrade = canUpgradePlan != null ? canUpgradePlan : switch (plan) {
      case free, individual, individual_pro -> true;
      case individual_max, business, enterprise -> false;
    };
    if (hasOverage && showUpgrade) {
      return List.of(manageOverage, upgradeSecondary);
    }
    if (hasOverage) {
      return List.of(manageOverage);
    }
    if (showUpgrade) {
      return List.of(upgradePrimary);
    }
    return List.of();
  }

  /**
   * Returns {@code true} when an error response represents a Bring-Your-Own-Key (BYOK) quota-exceeded
   * condition, i.e. a {@code 402} from the language server that also carries a non-blank model
   * provider name. BYOK usage is governed by the customer's provider account rather than the user's
   * Copilot plan, so callers should suppress the plan-driven
   * {@link #forPlan(CopilotPlan, boolean, Boolean)} actions and substitute the BYOK-specific
   * message in this case.
   *
   * @param code the error code from the language server
   * @param modelProviderName the BYOK model-provider name from the server payload, or {@code null}
   * @return {@code true} when the error should be rendered as a BYOK quota notice
   */
  public static boolean isByokQuotaExceeded(int code, String modelProviderName) {
    return code == 402 && StringUtils.isNotBlank(modelProviderName);
  }
}
