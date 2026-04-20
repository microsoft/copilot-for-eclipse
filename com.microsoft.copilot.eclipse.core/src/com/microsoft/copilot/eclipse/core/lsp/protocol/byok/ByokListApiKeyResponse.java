// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Response model for listing BYOK API keys.
 */
public class ByokListApiKeyResponse {
  private List<ByokApiKey> apiKeys;

  /**
   * Constructor for ByokListApiKeyResponse.
   *
   * @param apiKeys List of BYOK API keys.
   */
  public ByokListApiKeyResponse(List<ByokApiKey> apiKeys) {
    this.apiKeys = apiKeys;
  }

  public List<ByokApiKey> getApiKeys() {
    return apiKeys;
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiKeys);
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
    ByokListApiKeyResponse other = (ByokListApiKeyResponse) obj;
    return Objects.equals(apiKeys, other.apiKeys);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("apiKeys", apiKeys);
    return builder.toString();
  }
}
