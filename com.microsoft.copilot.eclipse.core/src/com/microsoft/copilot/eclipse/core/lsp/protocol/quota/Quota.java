package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Completions quota information.
 */
public class Quota {
  private double percentRemaining;
  private boolean unlimited;
  private boolean overagePermitted;
  private Integer entitlement;
  private Integer quotaRemaining;
  private String timeStamp;

  /**
   * Creates a new CompletionsQuota quota information with default values.
   */
  public Quota() {
    this.percentRemaining = 0.0;
    this.unlimited = false;
    this.overagePermitted = false;
  }

  /**
   * Gets the percentage of the quota remaining within the range of 0.0 to 100.0.
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

  /**
   * Gets the total entitlement (quota limit).
   */
  public Integer getEntitlement() {
    return entitlement;
  }

  public void setEntitlement(Integer entitlement) {
    this.entitlement = entitlement;
  }

  /**
   * Gets the remaining quota count.
   */
  public Integer getQuotaRemaining() {
    return quotaRemaining;
  }

  public void setQuotaRemaining(Integer quotaRemaining) {
    this.quotaRemaining = quotaRemaining;
  }

  /**
   * Gets the timestamp of the quota snapshot.
   */
  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entitlement, overagePermitted, percentRemaining, quotaRemaining, timeStamp, unlimited);
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
    Quota other = (Quota) obj;
    return Objects.equals(entitlement, other.entitlement) && overagePermitted == other.overagePermitted
        && Double.doubleToLongBits(percentRemaining) == Double.doubleToLongBits(other.percentRemaining)
        && Objects.equals(quotaRemaining, other.quotaRemaining) && Objects.equals(timeStamp, other.timeStamp)
        && unlimited == other.unlimited;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("percentRemaining", percentRemaining);
    builder.append("unlimited", unlimited);
    builder.append("overagePermitted", overagePermitted);
    builder.append("entitlement", entitlement);
    builder.append("quotaRemaining", quotaRemaining);
    builder.append("timeStamp", timeStamp);
    return builder.toString();
  }
}
