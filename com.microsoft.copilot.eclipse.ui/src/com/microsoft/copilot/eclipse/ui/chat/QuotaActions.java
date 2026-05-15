// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
   * Returns the ordered list of {@link QuotaAction}s appropriate for the supplied plan.
   *
   * <p>Mapping:
   * <ul>
   *   <li>{@code free} &rarr; "Upgrade Plan" (primary)</li>
   *   <li>{@code individual}, {@code individual_pro} &rarr; "Enable Additional Usage" /
   *       "Increase Budget" (primary) + "Upgrade Plan" (secondary)</li>
   *   <li>{@code individual_max} &rarr; "Enable Additional Usage" / "Increase Budget" (primary)</li>
   *   <li>{@code business}, {@code enterprise} &rarr; empty list</li>
   *   <li>{@code null} &rarr; empty list</li>
   * </ul>
   *
   * <p>The "Enable Additional Usage" label is replaced with "Increase Budget" when {@code overageEnabled}
   * is {@code true}, matching the IntelliJ quota dialog wording.
   *
   * @param plan the user's Copilot plan, or {@code null} when unknown
   * @param overageEnabled {@code true} when additional paid usage is already enabled for the user
   * @return an immutable, possibly empty list; never {@code null}
   */
  public static List<QuotaAction> forPlan(CopilotPlan plan, boolean overageEnabled) {
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
    return switch (plan) {
      case free -> List.of(upgradePrimary);
      case individual, individual_pro -> List.of(manageOverage, upgradeSecondary);
      case individual_max -> List.of(manageOverage);
      case business, enterprise -> List.of();
    };
  }

  /**
   * Returns {@code true} when an error response represents a Bring-Your-Own-Key (BYOK) quota-exceeded
   * condition, i.e. a {@code 402} from the language server that also carries a non-blank model
   * provider name. BYOK usage is governed by the customer's provider account rather than the user's
   * Copilot plan, so callers should suppress the plan-driven {@link #forPlan(CopilotPlan)} actions
   * and substitute the BYOK-specific message in this case.
   *
   * @param code the error code from the language server
   * @param modelProviderName the BYOK model-provider name from the server payload, or {@code null}
   * @return {@code true} when the error should be rendered as a BYOK quota notice
   */
  public static boolean isByokQuotaExceeded(int code, String modelProviderName) {
    return code == 402 && StringUtils.isNotBlank(modelProviderName);
  }
}
