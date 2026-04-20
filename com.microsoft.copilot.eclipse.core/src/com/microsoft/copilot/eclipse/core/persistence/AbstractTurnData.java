// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Base abstract turn data class that contains properties shared by all turn types. Base class for UserTurnData and
 * CopilotTurnData.
 */
public abstract class AbstractTurnData {
  protected String turnId;
  protected String role;
  protected Instant timestamp;
  protected Map<String, Object> data;

  public String getTurnId() {
    return turnId;
  }

  public void setTurnId(String turnId) {
    this.turnId = turnId;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getData() {
    return data;
  }

  public void setData(Map<String, Object> data) {
    this.data = data;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, role, timestamp, turnId);
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
    AbstractTurnData other = (AbstractTurnData) obj;
    return Objects.equals(data, other.data) && Objects.equals(role, other.role)
        && Objects.equals(timestamp, other.timestamp) && Objects.equals(turnId, other.turnId);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("turnId", turnId);
    builder.append("role", role);
    builder.append("timestamp", timestamp);
    builder.append("data", data);
    return builder.toString();
  }
}
