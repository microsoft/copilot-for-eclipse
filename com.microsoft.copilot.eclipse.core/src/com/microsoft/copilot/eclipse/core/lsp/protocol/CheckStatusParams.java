package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Parameter used for the checkStatus request.
 */
public class CheckStatusParams {

  private boolean localChecksOnly;

  private boolean forceRefresh;

  public boolean isLocalChecksOnly() {
    return localChecksOnly;
  }

  public void setLocalChecksOnly(boolean localChecksOnly) {
    this.localChecksOnly = localChecksOnly;
  }

  public boolean isForceRefresh() {
    return forceRefresh;
  }

  public void setForceRefresh(boolean forceRefresh) {
    this.forceRefresh = forceRefresh;
  }

  @Override
  public int hashCode() {
    return Objects.hash(forceRefresh, localChecksOnly);
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
    CheckStatusParams other = (CheckStatusParams) obj;
    return forceRefresh == other.forceRefresh && localChecksOnly == other.localChecksOnly;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("localChecksOnly", localChecksOnly);
    builder.add("forceRefresh", forceRefresh);
    return builder.toString();
  }
}
