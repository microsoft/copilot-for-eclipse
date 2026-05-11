// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol.quota;

import java.util.Objects;

/**
 * Quota information for a single tracked category (chat, completions, or premium interactions).
 *
 * <p>Equality intentionally excludes {@link #timeStamp} so that two snapshots with the same
 * display-meaningful state compare equal even when the language server stamps a different
 * production time on each refresh.
 *
 * @param percentRemaining percentage of the quota remaining; clamped into {@code [0.0, 100.0]} by
 *     the accessor since the language server may report drift slightly outside that range
 * @param unlimited whether this category has no monthly limit
 * @param overagePermitted whether the user has enabled additional paid usage beyond the allowance
 * @param overageCount additional paid units already consumed, when reported
 * @param entitlement total monthly allowance, when reported
 * @param quotaRemaining absolute units remaining in the monthly allowance, when reported
 * @param timeStamp ISO-8601 timestamp of when the snapshot was produced by the language server;
 *     not part of {@link #equals(Object)} / {@link #hashCode()}
 */
public record Quota(
    double percentRemaining,
    boolean unlimited,
    boolean overagePermitted,
    double overageCount,
    double entitlement,
    double quotaRemaining,
    String timeStamp) {

  /**
   * Returns the percentage of the quota remaining, clamped into the {@code [0.0, 100.0]} range.
   */
  public Quota {
    if (percentRemaining < 0.0) {
      percentRemaining = 0.0;
    } else if (percentRemaining > 100.0) {
      percentRemaining = 100.0;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Quota other)) {
      return false;
    }
    return Double.compare(percentRemaining, other.percentRemaining) == 0
        && unlimited == other.unlimited
        && overagePermitted == other.overagePermitted
        && Double.compare(overageCount, other.overageCount) == 0
        && Double.compare(entitlement, other.entitlement) == 0
        && Double.compare(quotaRemaining, other.quotaRemaining) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(percentRemaining, unlimited, overagePermitted, overageCount, entitlement, quotaRemaining);
  }
}
