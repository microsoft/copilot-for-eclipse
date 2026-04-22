// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

/**
 * Parameters for the "$/copilot/rateLimitWarning" notification.
 * Sent by the language server when usage rate limits are approaching thresholds.
 *
 * @param type the rate limit type ("weekly" or "session")
 * @param rateLimit the rate limit details
 * @param message the human-readable warning message
 */
public record RateLimitWarningParams(String type, RateLimit rateLimit, String message) {

  /**
   * Rate limit details including entitlement, remaining percentage, and reset date.
   *
   * @param entitlement the total entitlement
   * @param percentRemaining the percentage of quota remaining
   * @param resetDate the date when the rate limit resets
   */
  public record RateLimit(int entitlement, double percentRemaining, String resetDate) {
  }
}
