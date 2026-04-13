package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Quota information for Copilot usage (completions, chat, premium interactions, etc.).
 */
public class Quota {
  private double percentRemaining;
  private boolean unlimited;
  private boolean overagePermitted;
  @SerializedName("overage_count")
  private int overageCount;
  private int entitlement;
  private int quotaRemaining;
  @SerializedName("timeStamp")
  private String timestamp;

  /**
   * Gets the percentage of the quota remaining, clamped to [0.0, 100.0].
   */
  public double getPercentRemaining() {
    if (percentRemaining < 0.0) {
      return 0.0;
    } else if (percentRemaining > 100.0) {
      return 100.0;
    }
    return percentRemaining;
  }

  public void setPercentRemaining(double percentRemaining) {
    this.percentRemaining = percentRemaining;
  }

  public boolean isUnlimited() {
    return unlimited;
  }

  public void setUnlimited(boolean unlimited) {
    this.unlimited = unlimited;
  }

  public boolean isOveragePermitted() {
    return overagePermitted;
  }

  public void setOveragePermitted(boolean overagePermitted) {
    this.overagePermitted = overagePermitted;
  }

  public int getOverageCount() {
    return overageCount;
  }

  public void setOverageCount(int overageCount) {
    this.overageCount = overageCount;
  }

  public int getEntitlement() {
    return entitlement;
  }

  public void setEntitlement(int entitlement) {
    this.entitlement = entitlement;
  }

  public int getQuotaRemaining() {
    return quotaRemaining;
  }

  public void setQuotaRemaining(int quotaRemaining) {
    this.quotaRemaining = quotaRemaining;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entitlement, overagePermitted, overageCount, percentRemaining, quotaRemaining, timestamp,
        unlimited);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Quota other = (Quota) obj;
    return Double.doubleToLongBits(percentRemaining) == Double.doubleToLongBits(other.percentRemaining)
        && unlimited == other.unlimited && overagePermitted == other.overagePermitted
        && overageCount == other.overageCount && entitlement == other.entitlement
        && quotaRemaining == other.quotaRemaining && Objects.equals(timestamp, other.timestamp);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("percentRemaining", percentRemaining);
    builder.append("unlimited", unlimited);
    builder.append("overagePermitted", overagePermitted);
    builder.append("overageCount", overageCount);
    builder.append("entitlement", entitlement);
    builder.append("quotaRemaining", quotaRemaining);
    builder.append("timestamp", timestamp);
    return builder.toString();
  }
}
