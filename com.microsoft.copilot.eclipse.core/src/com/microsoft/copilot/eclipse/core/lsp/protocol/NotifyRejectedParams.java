package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameter used for the notify a completion rejection.
 */
public class NotifyRejectedParams {

  @NonNull
  private List<String> uuids;

  /**
   * Create a new NotifyRejectedParams.
   */
  public NotifyRejectedParams(List<String> uuids) {
    this.uuids = uuids;
  }

  public List<String> getUuids() {
    return uuids;
  }

  public void setUuids(List<String> uuids) {
    this.uuids = uuids;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuids);
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
    NotifyRejectedParams other = (NotifyRejectedParams) obj;
    return Objects.equals(uuids, other.uuids);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("uuids", uuids);
    return builder.toString();
  }

}
