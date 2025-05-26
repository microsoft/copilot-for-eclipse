package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Result of the checkQuota request.
 */
public class CheckQuotaResult {
  private ChatQuota chat;
  private CompletionsQuota completions;
  private PremiumInteractionsQuota premiumInteractions;
  private String resetDate;
  private CopilotPlan copilotPlan;

  public ChatQuota getChatQuota() {
    return chat;
  }

  public void setChatQuota(ChatQuota chat) {
    this.chat = chat;
  }

  public CompletionsQuota getCompletionsQuota() {
    return completions;
  }

  public void setCompletionsQuota(CompletionsQuota completions) {
    this.completions = completions;
  }

  public PremiumInteractionsQuota getPremiumInteractionsQuota() {
    return premiumInteractions;
  }

  public void setPremiumInteractionsQuota(PremiumInteractionsQuota premiumInteractions) {
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
