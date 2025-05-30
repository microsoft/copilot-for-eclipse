package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Result of the checkQuota request.
 */
public class CheckQuotaResult {
  private Quota chat;
  private Quota completions;
  private Quota premiumInteractions;
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
    return Objects.hash(chat, completions, copilotPlan, premiumInteractions, resetDate);
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
        && Objects.equals(resetDate, other.resetDate);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("chat", chat);
    builder.add("completions", completions);
    builder.add("premiumInteractions", premiumInteractions);
    builder.add("resetDate", resetDate);
    builder.add("copilotPlan", copilotPlan);
    return builder.toString();
  }
}
