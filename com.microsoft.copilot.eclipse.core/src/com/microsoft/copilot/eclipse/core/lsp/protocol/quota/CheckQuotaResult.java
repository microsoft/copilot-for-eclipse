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
  private IntervalQuota immediateUsageInterval;
  private IntervalQuota extendedUsageInterval;
  private String resetDate;
  private CopilotPlan copilotPlan;

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

  /**
   * Gets the immediate usage interval quota (for individual plans).
   */
  public IntervalQuota getImmediateUsageInterval() {
    return immediateUsageInterval;
  }

  public void setImmediateUsageInterval(IntervalQuota immediateUsageInterval) {
    this.immediateUsageInterval = immediateUsageInterval;
  }

  /**
   * Gets the extended usage interval quota (for individual plans).
   */
  public IntervalQuota getExtendedUsageInterval() {
    return extendedUsageInterval;
  }

  public void setExtendedUsageInterval(IntervalQuota extendedUsageInterval) {
    this.extendedUsageInterval = extendedUsageInterval;
  }

  public String getResetDate() {
    return resetDate;
  }

  public void setResetDate(String resetDate) {
    this.resetDate = resetDate;
  }

  public CopilotPlan getCopilotPlan() {
    return copilotPlan;
  }

  public void setCopilotPlan(CopilotPlan copilotPlan) {
    this.copilotPlan = copilotPlan;
  }

  @Override
  public int hashCode() {
    return Objects.hash(chat, completions, copilotPlan, extendedUsageInterval,
        immediateUsageInterval, premiumInteractions, resetDate);
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
        && copilotPlan == other.copilotPlan
        && Objects.equals(extendedUsageInterval, other.extendedUsageInterval)
        && Objects.equals(immediateUsageInterval, other.immediateUsageInterval)
        && Objects.equals(premiumInteractions, other.premiumInteractions)
        && Objects.equals(resetDate, other.resetDate);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("chat", chat);
    builder.append("completions", completions);
    builder.append("premiumInteractions", premiumInteractions);
    builder.append("immediateUsageInterval", immediateUsageInterval);
    builder.append("extendedUsageInterval", extendedUsageInterval);
    builder.append("resetDate", resetDate);
    builder.append("copilotPlan", copilotPlan);
    return builder.toString();
  }
}
