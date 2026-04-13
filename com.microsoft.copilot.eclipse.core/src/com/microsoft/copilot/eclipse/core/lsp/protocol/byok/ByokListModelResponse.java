// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.byok;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Response model for listing BYOK models.
 */
public class ByokListModelResponse {
  private List<ByokModel> models;

  public List<ByokModel> getModels() {
    return models;
  }

  public void setModels(List<ByokModel> models) {
    this.models = models;
  }

  @Override
  public int hashCode() {
    return Objects.hash(models);
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
    ByokListModelResponse other = (ByokListModelResponse) obj;
    return Objects.equals(models, other.models);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("models", models);
    return builder.toString();
  }
}
