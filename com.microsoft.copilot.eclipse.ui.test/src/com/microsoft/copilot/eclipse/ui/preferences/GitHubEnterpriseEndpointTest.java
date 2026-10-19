// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GitHubEnterpriseEndpointTest {

  @ParameterizedTest
  @CsvSource({
      "company.ghe.com, https://company.ghe.com",
      "company.ghe.com/, https://company.ghe.com/",
      "http://company.ghe.com, https://company.ghe.com",
      "http://company.ghe.com/, https://company.ghe.com/",
      "https://company.ghe.com, https://company.ghe.com",
      "HTTPS://company.ghe.com, https://company.ghe.com",
      "HtTp://company.ghe.com, https://company.ghe.com",
      "' https://company.ghe.com ', https://company.ghe.com",
      "' company.ghe.com ', https://company.ghe.com",
      "'   ', ''",
      "github.company.example:8443, https://github.company.example:8443",
      "http://github.company.example:8080/copilot, https://github.company.example:8080/copilot",
      "https://company.ghe.com/login%2Foauth?a=1&b=2#section, https://company.ghe.com/login%2Foauth?a=1&b=2#section"
  })
  void testNormalize_validEndpoint_returnsHttpsUrl(String input, String expected) {
    assertEquals(expected, GitHubEnterpriseEndpoint.normalize(input));
    assertEquals(expected, GitHubEnterpriseEndpoint.normalize(expected));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void testNormalize_noEndpoint_preservesGitHubComDefault(String input) {
    assertEquals(input, GitHubEnterpriseEndpoint.normalize(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ftp://company.ghe.com", "file://company.ghe.com", "https://", "http://",
      "https:///company.ghe.com", "https://company ghe.com", "company ghe.com", "https://company.ghe.com/%",
      "https://company.ghe.com:invalid", "company.ghe.com:invalid", "//company.ghe.com"})
  void testNormalize_invalidEndpoint_throws(String input) {
    assertThrows(IllegalArgumentException.class, () -> GitHubEnterpriseEndpoint.normalize(input));
  }
}
