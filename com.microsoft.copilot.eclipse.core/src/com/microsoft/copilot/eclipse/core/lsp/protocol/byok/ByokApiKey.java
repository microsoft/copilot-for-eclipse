// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Model representing a BYOK (Bring Your Own Key) API key configuration.
 */
public class ByokApiKey {

  private String providerName;
  private String modelId;
  private String apiKey;

  /**
   * Constructor with parameters.
   *
   * @param providerName The name of the provider.
   * @param modelId      The ID of the model.
   */
  public ByokApiKey(String providerName, String modelId) {
    this.providerName = providerName;
    this.modelId = modelId;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerName, modelId, apiKey);
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
    ByokApiKey other = (ByokApiKey) obj;
    return Objects.equals(providerName, other.providerName) && Objects.equals(modelId, other.modelId)
        && Objects.equals(apiKey, other.apiKey);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("providerName", providerName);
    builder.append("modelId", modelId);
    builder.append("apiKey", apiKey);
    return builder.toString();
  }
}
