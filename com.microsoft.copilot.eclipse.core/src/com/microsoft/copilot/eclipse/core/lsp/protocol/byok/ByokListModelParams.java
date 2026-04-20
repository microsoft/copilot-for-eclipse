// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Parameters for listing BYOK models.
 */
public class ByokListModelParams {

  private String providerName;
  private Boolean enableFetchUrl;

  /**
   * Default constructor.
   */
  public ByokListModelParams(String providerName, Boolean enableFetchUrl) {
    this.providerName = providerName;
    this.enableFetchUrl = enableFetchUrl;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public Boolean getEnableFetchUrl() {
    return enableFetchUrl;
  }

  public void setEnableFetchUrl(Boolean enableFetchUrl) {
    this.enableFetchUrl = enableFetchUrl;
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerName, enableFetchUrl);
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
    ByokListModelParams other = (ByokListModelParams) obj;
    return Objects.equals(providerName, other.providerName) && Objects.equals(enableFetchUrl, other.enableFetchUrl);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("providerName", providerName);
    builder.append("enableFetchUrl", enableFetchUrl);
    return builder.toString();
  }
}
