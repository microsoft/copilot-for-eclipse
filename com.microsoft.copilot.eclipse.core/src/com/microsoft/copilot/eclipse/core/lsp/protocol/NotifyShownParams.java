package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * * Parameters for the notifyShown request.
 */
public class NotifyShownParams {

  @NonNull
  private String uuid;

  /**
   * Creates a new NotifyShownParams.
   */
  public NotifyShownParams(String uuid) {
    this.uuid = uuid;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
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
    NotifyShownParams other = (NotifyShownParams) obj;
    return Objects.equals(uuid, other.uuid);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("uuid", uuid);
    return builder.toString();
  }

}
