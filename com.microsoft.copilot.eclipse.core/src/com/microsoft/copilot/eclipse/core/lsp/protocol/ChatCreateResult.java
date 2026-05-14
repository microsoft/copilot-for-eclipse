// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Result of a chat creation.
 */
public class ChatCreateResult {
  @NonNull
  private String conversationId;
  @NonNull
  private String turnId;
  private String agentSlug;
  private String modelName;
  private String modelProviderName;
  private double billingMultiplier;

  public String getConversationId() {
    return conversationId;
  }

  public String getTurnId() {
    return turnId;
  }

  public String getAgentSlug() {
    return agentSlug;
  }

  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public void setAgentSlug(String agentSlug) {
    this.agentSlug = agentSlug;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getModelProviderName() {
    return modelProviderName;
  }

  public void setModelProviderName(String modelProviderName) {
    this.modelProviderName = modelProviderName;
  }

  public double getBillingMultiplier() {
    return billingMultiplier;
  }

  public void setBillingMultiplier(double billingMultiplier) {
    this.billingMultiplier = billingMultiplier;
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentSlug, billingMultiplier, conversationId, modelName, modelProviderName, turnId);
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
    ChatCreateResult other = (ChatCreateResult) obj;
    return Objects.equals(agentSlug, other.agentSlug)
        && Double.doubleToLongBits(billingMultiplier) == Double.doubleToLongBits(other.billingMultiplier)
        && Objects.equals(conversationId, other.conversationId) && Objects.equals(modelName, other.modelName)
        && Objects.equals(modelProviderName, other.modelProviderName) && Objects.equals(turnId, other.turnId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("conversationId", conversationId);
    builder.append("turnId", turnId);
    builder.append("agentSlug", agentSlug);
    builder.append("modelName", modelName);
    builder.append("modelProviderName", modelProviderName);
    builder.append("billingMultiplier", billingMultiplier);
    return builder.toString();
  }

}
