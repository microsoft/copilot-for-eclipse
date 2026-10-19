// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;

/**
 * Normalizes GitHub Enterprise authentication endpoints before saving or using them.
 */
final class GitHubEnterpriseEndpoint {

  private GitHubEnterpriseEndpoint() {
  }

  /**
   * Resolves a hostname or HTTP/HTTPS URL to HTTPS, preserving the rest of the URL.
   *
   * @param endpoint the configured endpoint; null or blank selects GitHub.com
   * @return the HTTPS endpoint, or the original null/trimmed empty value for GitHub.com
   * @throws IllegalArgumentException if the endpoint is not a valid HTTP/HTTPS URL with a hostname
   */
  static String normalize(String endpoint) {
    String value = StringUtils.trim(endpoint);
    if (StringUtils.isEmpty(value)) {
      return value;
    }
    if (!value.contains("://")) {
      value = "https://" + value;
    }

    URI uri = URI.create(value);
    if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
        || StringUtils.isBlank(uri.getHost())) {
      throw new IllegalArgumentException("Expected a GitHub Enterprise hostname or HTTP/HTTPS URL.");
    }
    return "https" + value.substring(uri.getScheme().length());
  }
}
