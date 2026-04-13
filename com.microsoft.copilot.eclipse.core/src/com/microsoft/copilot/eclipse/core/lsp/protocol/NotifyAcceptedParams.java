package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Parameter used for the notify a completion acceptance.
 */
public class NotifyAcceptedParams {

  @NonNull
  private String uuid;

  private Integer acceptedLength;

  /**
   * Create a new NotifyAcceptedParams.
   */
  public NotifyAcceptedParams(String uuid) {
    super();
    this.uuid = uuid;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Integer getAcceptedLength() {
    return acceptedLength;
  }

  public void setAcceptedLength(Integer acceptedLength) {
    this.acceptedLength = acceptedLength;
  }

  @Override
  public int hashCode() {
    return Objects.hash(acceptedLength, uuid);
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
    NotifyAcceptedParams other = (NotifyAcceptedParams) obj;
    return acceptedLength == other.acceptedLength && Objects.equals(uuid, other.uuid);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("uuid", uuid);
    builder.append("acceptedLength", acceptedLength);
    return builder.toString();
  }

}
