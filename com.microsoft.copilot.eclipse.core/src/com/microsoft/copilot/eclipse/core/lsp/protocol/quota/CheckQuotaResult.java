// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of the checkQuota request.
 */
public class CheckQuotaResult {
  private Quota chat;
  private Quota completions;
  private Quota premiumInteractions;

  /**
   * Quota usage for the 5-hour rolling interval (session limit). Only included for CFI (Copilot for Individuals)
   * token-based billing users
   */
  private TbbQuota immediateUsageInterval;

  /**
   * Quota usage for the 7-day rolling interval (weekly limit). Only included for CFI (Copilot for Individuals)
   * token-based billing users (all CFI plans).
   */
  private TbbQuota extendedUsageInterval;
  private String resetDate;
  private String resetDateUtc;
  private CopilotPlan copilotPlan;
  private boolean tokenBasedBillingEnabled;

  public Quota getChatQuota() {
    return chat;
  }

  public void setChatQuota(Quota chat) {
    this.chat = chat;
  }

  public Quota getCompletionsQuota() {
    return completions;
  }

  public void setCompletionsQuota(Quota completions) {
    this.completions = completions;
  }

  public Quota getPremiumInteractionsQuota() {
    return premiumInteractions;
  }

  public void setPremiumInteractionsQuota(Quota premiumInteractions) {
    this.premiumInteractions = premiumInteractions;
  }

  public String getResetDate() {
    return resetDate;
  }

  public void setResetDate(String resetDate) {
    this.resetDate = resetDate;
  }

  public String getResetDateUtc() {
    return resetDateUtc;
  }

  public void setResetDateUtc(String resetDateUtc) {
    this.resetDateUtc = resetDateUtc;
  }

  public CopilotPlan getCopilotPlan() {
    return copilotPlan;
  }

  public void setCopilotPlan(CopilotPlan copilotPlan) {
    this.copilotPlan = copilotPlan;
  }

  public TbbQuota getImmediateUsageInterval() {
    return immediateUsageInterval;
  }

  public void setImmediateUsageInterval(TbbQuota immediateUsageInterval) {
    this.immediateUsageInterval = immediateUsageInterval;
  }

  public TbbQuota getExtendedUsageInterval() {
    return extendedUsageInterval;
  }

  public void setExtendedUsageInterval(TbbQuota extendedUsageInterval) {
    this.extendedUsageInterval = extendedUsageInterval;
  }

  public boolean isTokenBasedBillingEnabled() {
    return tokenBasedBillingEnabled;
  }

  public void setTokenBasedBillingEnabled(boolean tokenBasedBillingEnabled) {
    this.tokenBasedBillingEnabled = tokenBasedBillingEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chat, completions, copilotPlan, extendedUsageInterval, immediateUsageInterval,
        premiumInteractions, resetDate, resetDateUtc, tokenBasedBillingEnabled);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CheckQuotaResult other = (CheckQuotaResult) obj;
    return Objects.equals(chat, other.chat) && Objects.equals(completions, other.completions)
        && copilotPlan == other.copilotPlan && Objects.equals(premiumInteractions, other.premiumInteractions)
        && Objects.equals(resetDate, other.resetDate) && Objects.equals(resetDateUtc, other.resetDateUtc)
        && Objects.equals(immediateUsageInterval, other.immediateUsageInterval)
        && Objects.equals(extendedUsageInterval, other.extendedUsageInterval)
        && tokenBasedBillingEnabled == other.tokenBasedBillingEnabled;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("chat", chat);
    builder.append("completions", completions);
    builder.append("premiumInteractions", premiumInteractions);
    builder.append("resetDate", resetDate);
    builder.append("resetDateUtc", resetDateUtc);
    builder.append("copilotPlan", copilotPlan);
    builder.append("immediateUsageInterval", immediateUsageInterval);
    builder.append("extendedUsageInterval", extendedUsageInterval);
    builder.append("tokenBasedBillingEnabled", tokenBasedBillingEnabled);
    return builder.toString();
  }

  /**
   * Updates this result with fields from a push quota change notification. Fields not present in the notification
   * (chat, completions, resetDate) are left unchanged.
   *
   * @param notification the push notification from the language server
   */
  public void updateFromNotification(QuotaChangeNotification notification) {
    if (notification == null) {
      return;
    }
    if (notification.premiumInteractions() != null) {
      this.premiumInteractions = notification.premiumInteractions().toQuota();
    }
    if (notification.immediateUsageInterval() != null) {
      this.immediateUsageInterval = notification.immediateUsageInterval().toTbbQuota();
    }
    if (notification.extendedUsageInterval() != null) {
      this.extendedUsageInterval = notification.extendedUsageInterval().toTbbQuota();
    }
    if (notification.copilotPlan() != null) {
      this.copilotPlan = notification.copilotPlan();
    }
  }
}
